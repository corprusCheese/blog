package blog.storage

import blog.domain._
import blog.domain.tags._
import blog.storage.combined.CreateUpdateDelete

trait TagStorageDsl[F[_]]
    extends CreateUpdateDelete[F, TagCreate, TagUpdate, TagDelete] {

  def findById(id: TagId): F[Option[Tag]]
  def fetchAll: F[Vector[Tag]]
  def findByName(name: TagName): F[Vector[Tag]]
  def getAllPostTags(postId: PostId): F[Vector[Tag]]

}
