package impl

import blog.domain
import blog.domain.tags._
import blog.storage.TagStorageDsl
import cats.effect.{MonadCancelThrow, Resource}

case class TestTagStorage[F[_]: MonadCancelThrow] () extends TagStorageDsl[F] {

  private var inMemoryVector: Vector[Tag] = Vector.empty

  override def findById(id: domain.TagId): F[Option[Tag]] = ???

  override def fetchAll: F[Vector[Tag]] = ???

  override def findByName(name: domain.TagName): F[Vector[Tag]] = ???

  override def getAllPostTags(postId: domain.PostId): F[Vector[Tag]] = ???

  override def delete(delete: TagDelete): F[Unit] = ???

  override def create(create: TagCreate): F[Unit] = ???

  override def update(update: TagUpdate): F[Unit] = ???
}

object TestTagStorage {
  def resource[F[_]: MonadCancelThrow]: Resource[F, TagStorageDsl[F]] =
    Resource.pure[F, TagStorageDsl[F]](
      TestTagStorage[F]()
    )
}