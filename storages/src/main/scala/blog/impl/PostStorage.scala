package blog.impl

import blog.domain
import blog.domain.{Page, PerPage, posts}
import blog.storage.PostStorageDsl
import cats.effect.Resource
import doobie.util.transactor

class PostStorage[F[_]] extends PostStorageDsl[F] {
  override def findById(id: domain.PostId): F[Option[posts.Post]] = ???

  override def fetchForPagination(page: Page, perPage: PerPage): F[Vector[posts.Post]] = ???

  override def getAllUserPosts(userId: domain.UserId): F[Vector[posts.Post]] = ???

  override def getPostsWithTagsWithPagination(tagId: domain.TagId, page: Page, perPage: PerPage): F[Vector[posts.Post]] = ???

  override def create(create: posts.CreatePost): F[Unit] = ???

  override def update(update: posts.UpdatePost): F[Unit] = ???

  override def delete(delete: posts.DeletePost): F[Unit] = ???
}

object PostStorage {
  def make[F[_]](ta: transactor.Transactor[F]): Resource[F, PostStorageDsl[F]] = ???
}
