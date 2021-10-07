package blog.programs

import blog.domain._
import blog.domain.comments._
import blog.errors._
import blog.storage._
import cats.MonadThrow
import cats.implicits._
import org.http4s.circe.JsonDecoder

import java.util.UUID

trait CommentProgram[F[_]] {
  def create(
      message: CommentMessage,
      userId: UserId,
      postId: PostId,
      commentId: Option[CommentId]
  ): F[Unit]
  def update(
      commentId: CommentId,
      message: CommentMessage,
      userId: UserId
  ): F[Unit]
  def delete(commentId: CommentId, userId: UserId): F[Unit]
  def getAll: F[Vector[CustomComment]]
  def findById(commentId: CommentId): F[Option[CustomComment]]
  def getAllPostComments(postId: PostId): F[Vector[CustomComment]]
  def getActiveUserComments(userId: UserId): F[Vector[CustomComment]]
}

object CommentProgram {
  def make[F[_]: JsonDecoder: MonadThrow](
      commentStorage: CommentStorageDsl[F]
  ): CommentProgram[F] =
    new CommentProgram[F] {
      override def create(
          message: CommentMessage,
          userId: UserId,
          postId: PostId,
          commentId: Option[CommentId]
      ): F[Unit] = {
        getPath(postId, commentId).flatMap( path =>
        commentStorage
          .create(
            CreateComment(
              CommentId(UUID.randomUUID()),
              message,
              userId,
              path
            )
          ))
          .handleErrorWith(_ => throw CreateCommentError)
      }

      override def update(
          commentId: CommentId,
          message: CommentMessage,
          userId: UserId
      ): F[Unit] =
        commentBelongsToUser(userId, commentId).flatMap {
          case None => throw CommentDontBelongToUser
          case Some(comment: Comment) =>
            commentStorage
              .update(
                UpdateComment(
                  comment.commentId,
                  comment.message
                )
              )
              .handleErrorWith(_ => throw UpdateCommentError)
          case Some(_: DeletedComment) => throw CommentNotExists
        }

      override def delete(commentId: CommentId, userId: UserId): F[Unit] =
        commentBelongsToUser(userId, commentId)
          .flatMap {
            case None => throw CommentNotExists
            case Some(comment: Comment) =>
              commentStorage
                .delete(DeleteComment(comment.commentId))
                .handleErrorWith(_ => throw DeleteCommentError)

            case Some(_: DeletedComment) => throw CommentAlreadyDeleted
          }

      override def getAll: F[Vector[CustomComment]] = commentStorage.fetchAll

      override def findById(commentId: CommentId): F[Option[CustomComment]] =
        commentStorage.findById(commentId)

      override def getAllPostComments(
          postId: PostId
      ): F[Vector[CustomComment]] = commentStorage.getAllPostComments(postId)

      override def getActiveUserComments(
          userId: UserId
      ): F[Vector[CustomComment]] = commentStorage.getActiveUserComments(userId)

      private def commentBelongsToUser(
          userId: UserId,
          commentId: CommentId
      ): F[Option[CustomComment]] =
        commentStorage.findById(commentId).map {
          case None => none[CustomComment]
          case Some(comment) =>
            if (comment.userId == userId) comment.some else none[CustomComment]
        }

      private def getPath(
          postId: PostId,
          commentIdOpt: Option[CommentId]
      ): F[CommentMaterializedPath] =
        commentIdOpt match {
          case None => CommentMaterializedPath(postId.toString).pure[F]
          case Some(commentId) =>
            commentStorage.findById(commentId).map {
              case None          => throw NoSuchCommentId
              case Some(comment) => CommentMaterializedPath(comment.path + "." + commentId)
            }
        }
    }
}
