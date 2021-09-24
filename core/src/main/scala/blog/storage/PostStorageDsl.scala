package blog.storage

import blog.domain._
import blog.domain.posts._
import blog.storage.combined.CreateUpdateDelete

trait PostStorageDsl[F[_]]
    extends CreateUpdateDelete[F, CreatePost, UpdatePost, DeletePost] {

  def findById(id: PostId): F[Option[Post]]
  def fetchForPagination(page: Page, perPage: PerPage): F[Vector[Post]]
  def getAllUserPosts(userId: UserId): F[Vector[Post]]
  def getPostsWithTagsWithPagination(
      tagId: TagId,
      page: Page,
      perPage: PerPage
  ): F[Vector[Post]]
}
