package blog.impl

import blog.domain
import blog.domain.tags
import blog.storage.TagStorageDsl
import cats.effect.Resource
import doobie.util.transactor

class TagStorage[F[_]] extends TagStorageDsl[F]{
  override def findById(id: domain.CommentId): F[Option[tags.Tag]] = ???

  override def fetchAll: F[Vector[tags.Tag]] = ???

  override def findByName(name: domain.TagName): F[Vector[tags.Tag]] = ???

  override def getAllPostTags(postId: domain.PostId): F[Vector[tags.Tag]] = ???

  override def create(create: tags.CreateTag): F[Unit] = ???

  override def update(update: tags.UpdateTag): F[Unit] = ???

  override def delete(delete: tags.DeleteTag): F[Unit] = ???
}

object TagStorage {
  def make[F[_]](ta: transactor.Transactor[F]): Resource[F, TagStorageDsl[F]] = ???
}
