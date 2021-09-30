package blog.storage

import blog.domain._
import blog.domain.comments._
import blog.domain.posts.Post
import blog.storage.combined.CreateUpdateDelete

trait CommentStorageDsl[F[_]]
    extends CreateUpdateDelete[F, CreateComment, UpdateComment, DeleteComment] {

  def findById(id: CommentId): F[Option[Comment]]
  def fetchAll: F[Vector[Comment]]
  def getAllUserComments(userId: UserId): F[Vector[Comment]]
  def getAllPostComments(postId: PostId): F[Vector[Comment]]
  def deleteAllPostComments(postId: PostId): F[Unit]
}
