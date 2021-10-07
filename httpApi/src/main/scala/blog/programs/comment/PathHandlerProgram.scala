package blog.programs.comment

import blog.domain._
import blog.errors.NoSuchCommentId
import blog.storage.CommentStorageDsl
import cats._
import cats.implicits._

trait PathHandlerProgram[F[_]] {
  def getPath(
      postId: PostId,
      commentIdOpt: Option[CommentId]
  ): F[CommentMaterializedPath]
}

object PathHandlerProgram {
  def make[F[_]: Monad](
      commentStorage: CommentStorageDsl[F]
  ): PathHandlerProgram[F] =
    new PathHandlerProgram[F] {
      override def getPath(
          postId: PostId,
          commentIdOpt: Option[CommentId]
      ): F[CommentMaterializedPath] =
        commentIdOpt match {
          case None => CommentMaterializedPath(postId.toString).pure[F]
          case Some(commentId) =>
            commentStorage.findById(commentId).map {
              case None => throw NoSuchCommentId
              case Some(comment) =>
                CommentMaterializedPath(comment.path + "." + commentId)
            }
        }
    }
}
