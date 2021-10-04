package blog.impl

import blog.domain._
import blog.domain.comments._
import blog.storage.CommentStorageDsl
import cats.effect.{MonadCancelThrow, Resource}
import doobie.util.transactor
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import eu.timepit.refined.auto._
import blog.meta._
import doobie.implicits._
import cats.implicits._
import doobie.postgres.circe.json.implicits
import doobie.postgres.circe.jsonb.implicits
import doobie.Fragments

case class CommentStorage[F[_]: Logger: MonadCancelThrow](tx: Transactor[F])
    extends CommentStorageDsl[F] {
  override def findById(id: CommentId): F[Option[Comment]] =
    sql"""SELECT uuid, message, user_id, ltree2text(comment_path), deleted FROM comments WHERE uuid = ${id}"""
      .query[
        (CommentId, CommentMessage, UserId, CommentMaterializedPath, Deleted)
      ]
      .option
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding comment by id = ${id}"))
      .map(_.map {
        case (commentId, message, userId, path, deleted) =>
          Comment(commentId, message, userId, path, deleted)
      })

  override def fetchAll: F[Vector[Comment]] =
    sql"""SELECT uuid, message, user_id, ltree2text(comment_path), deleted FROM comments"""
      .query[
        (CommentId, CommentMessage, UserId, CommentMaterializedPath, Deleted)
      ]
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding all comments"))
      .map(_.map {
        case (commentId, message, userId, path, deleted) =>
          Comment(commentId, message, userId, path, deleted)
      })

  override def getAllUserComments(
      userId: UserId
  ): F[Vector[Comment]] =
    sql"""SELECT uuid, message, user_id, ltree2text(comment_path), deleted FROM comments WHERE user_id = ${userId}"""
      .query[
        (CommentId, CommentMessage, UserId, CommentMaterializedPath, Deleted)
      ]
      .to[Vector]
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"finding comments of user id = ${userId}")
      )
      .map(_.map {
        case (commentId, message, userId, path, deleted) =>
          Comment(commentId, message, userId, path, deleted)
      })

  override def getAllPostComments(
      postId: PostId
  ): F[Vector[Comment]] =
    sql"""SELECT uuid, message, user_id, ltree2text(comment_path), deleted FROM comments WHERE comment_path <@ text2ltree(${CommentMaterializedPath(postId.toString)})"""
      .query[
        (CommentId, CommentMessage, UserId, CommentMaterializedPath, Deleted)
      ]
      .to[Vector]
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"finding comments of post id = ${postId}")
      )
      .map(_.map {
        case (commentId, message, userId, path, deleted) =>
          Comment(commentId, message, userId, path, deleted)
      })

  override def update(update: UpdateComment): F[Unit] =
    sql"""UPDATE comments SET message = ${update.message} WHERE uuid = ${update.commentId}""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F]
          .info(s"updating post with id = ${update.commentId}")
      )
      .map(_ => ())

  override def delete(delete: DeleteComment): F[Unit] =
    sql"""UPDATE comments SET deleted = true WHERE uuid = ${delete.commentId}""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F]
          .info(s"deleting post with id = ${delete.commentId}")
      )
      .map(_ => ())

  override def create(create: CreateComment): F[Unit] =
    sql"""INSERT INTO comments (uuid, message, user_id, comment_path) VALUES (${create.commentId}, ${create.message}, ${create.userId}, text2ltree(${create.path}))""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F]
          .info("creating new comment")
      )
      .map(_ => ())

  override def deleteAllPostComments(postId: PostId): F[Unit] =
    sql"""UPDATE comments SET deleted = true WHERE comment_path <@ text2ltree(${CommentMaterializedPath(postId.toString)})"""
      .update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"delete all comments of post id = ${postId}")
      )
      .map(_ => ())
}

object CommentStorage {
  def resource[F[_]: Logger: MonadCancelThrow](
      tx: transactor.Transactor[F]
  ): Resource[F, CommentStorageDsl[F]] =
    Resource.pure[F, CommentStorage[F]](CommentStorage[F](tx))
}
