package blog.impl

import blog.domain
import blog.domain.comments
import blog.storage.CommentStorageDsl
import cats.effect.Resource
import doobie.util.transactor

class CommentStorage[F[_]] extends CommentStorageDsl[F] {
  override def findById(id: domain.CommentId): F[Option[comments.Comment]] = ???

  override def fetchAll: F[Vector[comments.Comment]] = ???

  override def getAllUserComments(userId: domain.UserId): F[Vector[comments.Comment]] = ???

  override def getAllPostComments(postId: domain.PostId): F[Vector[comments.Comment]] = ???

  override def update(update: comments.UpdateComment): F[Unit] = ???

  override def delete(delete: comments.DeleteComment): F[Unit] = ???

  override def create(create: comments.CreateComment): F[Unit] = ???
}

object CommentStorage {
  def make[F[_]](ta: transactor.Transactor[F]): Resource[F, CommentStorageDsl[F]] = ???
}
