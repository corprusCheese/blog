package impl

import blog.domain
import blog.domain._
import blog.domain.comments._
import blog.storage.{CommentStorageDsl, PostStorageDsl}
import cats.effect.{MonadCancelThrow, Resource}

case class TestCommentStorage[F[_]: MonadCancelThrow]() extends CommentStorageDsl[F] {
  override def findById(id: domain.CommentId): F[Option[CustomComment]] = ???

  override def fetchAll: F[Vector[CustomComment]] = ???

  override def getActiveUserComments(userId: UserId): F[Vector[CustomComment]] = ???

  override def getAllPostComments(postId: PostId): F[Vector[CustomComment]] = ???

  override def deleteAllPostComments(postId: PostId): F[Unit] = ???

  override def delete(delete: DeleteComment): F[Unit] = ???

  override def create(create: CreateComment): F[Unit] = ???

  override def update(update: UpdateComment): F[Unit] = ???
}

object TestPostStorage {
  def resource[F[_]: MonadCancelThrow]: Resource[F, CommentStorageDsl[F]] =
    Resource.pure[F, CommentStorageDsl[F]](
      TestCommentStorage[F]()
    )
}
