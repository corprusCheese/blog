package blog.storage

import blog.domain._
import blog.domain.posts._
import blog.storage.combined.CreateUpdateDelete
import cats.data.NonEmptyVector

trait PostStorageDsl[F[_]]
    extends CreateUpdateDelete[F, CreatePost, UpdatePost, DeletePost] {

  def findById(id: PostId): F[Option[Post]]
  def fetchForPagination(page: Page): F[Vector[Post]]
  def getAllUserPosts(userId: UserId): F[Vector[Post]]
  def fetchPostForPaginationWithTags(
      tagIds: NonEmptyVector[TagId],
      page: Page
  ): F[Vector[Post]]
}
