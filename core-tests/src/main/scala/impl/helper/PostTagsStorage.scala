package impl.helper

import blog.domain._
import cats.Monad
import cats.effect.MonadCancelThrow
import cats.implicits.catsSyntaxApplicativeId

object PostTagsStorage {
  private var inMemoryVector: Vector[(PostId, TagId)] = Vector.empty

  def create[F[_]: Monad](postId: PostId, tagId: TagId): F[Unit] = {
    inMemoryVector = inMemoryVector :+ (postId, tagId)
    Monad[F].unit
  }

  def deletePostId[F[_]: Monad](postId: PostId): F[Unit] = {
    inMemoryVector = inMemoryVector.filter(_._1 != postId)
    Monad[F].unit
  }

  def deleteTagId[F[_]: Monad](tagId: TagId): F[Unit] = {
    inMemoryVector = inMemoryVector.filter(_._2 != tagId)
    Monad[F].unit
  }

  def findByTagId[F[_]: Monad](tagId: TagId): F[Vector[PostId]] =
    inMemoryVector.filter(_._2 == tagId).map(_._1).pure[F]

  def findByPostId[F[_]: Monad](postId: PostId): F[Vector[TagId]] =
    inMemoryVector.filter(_._1 == postId).map(_._2).pure[F]

}
