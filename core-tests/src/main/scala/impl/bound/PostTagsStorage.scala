package impl.bound

import blog.domain._
import cats.Monad
import cats.effect.{Ref, Resource}
import cats.implicits._
import eu.timepit.refined.auto._

trait PostTagsStorage[F[_]] {
  def create(postId: PostId, tagId: TagId): F[Unit]
  def create(postId: PostId, tagIds: Vector[TagId]): F[Unit]
  def create(postIds: Vector[PostId], tagId: TagId): F[Unit]
  def deletePostId(postId: PostId): F[Unit]
  def deleteTagId(tagId: TagId): F[Unit]
  def findByTagId(tagId: TagId): F[Vector[PostId]]
  def findByPostId(postId: PostId): F[Vector[TagId]]
  def getAll: F[Vector[(PostId, TagId)]]
}

object PostTagsStorage {
  private def make[F[_]: Monad: Ref.Make]: F[PostTagsStorage[F]] =
    Ref
      .of[F, Vector[(PostId, TagId)]](Vector.empty)
      .map(inMemoryVector =>
        new PostTagsStorage[F] {
          override def create(postId: PostId, tagId: TagId): F[Unit] =
            for {
              get <- inMemoryVector.get
              newVector = get :+ (postId, tagId)
              _ <- inMemoryVector.set(newVector)
            } yield ()

          override def deletePostId(postId: PostId): F[Unit] =
            for {
              get <- inMemoryVector.get
              newVector = get.filter(_._1 != postId)
              _ <- inMemoryVector.set(newVector)
            } yield ()

          override def deleteTagId(tagId: TagId): F[Unit] =
            for {
              get <- inMemoryVector.get
              newVector = get.filter(_._2 != tagId)
              _ <- inMemoryVector.set(newVector)
            } yield ()

          override def findByTagId(tagId: TagId): F[Vector[PostId]] =
            inMemoryVector.get.map(_.filter(_._2 == tagId).map(_._1))

          override def findByPostId(postId: PostId): F[Vector[TagId]] =
            inMemoryVector.get.map(_.filter(_._1 == postId).map(_._2))

          override def create(postId: PostId, tagIds: Vector[TagId]): F[Unit] =
            tagIds
              .map(tagId => create(postId, tagId))
              .sequence.map(_ => ())

          override def create(postIds: Vector[PostId], tagId: TagId): F[Unit] =
            postIds
              .map(postId => create(postId, tagId))
              .sequence.map(_ => ())

          override def getAll: F[Vector[(PostId, TagId)]] = inMemoryVector.get
        }
      )

  def resource[F[_]: Monad: Ref.Make]: Resource[F, PostTagsStorage[F]] =
    Resource.eval[F, PostTagsStorage[F]](PostTagsStorage.make[F])
}
