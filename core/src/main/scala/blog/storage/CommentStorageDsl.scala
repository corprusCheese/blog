package blog.storage

import blog.domain._
import blog.domain.comments._
import blog.domain.posts.Post
import blog.storage.combined.CreateUpdateDelete

trait CommentStorageDsl[F[_]]
    extends CreateUpdateDelete[F, CreateComment, UpdateComment, DeleteComment] {

  def findById(id: CommentId): F[Option[CustomComment]]
  def fetchAll: F[Vector[CustomComment]]
  def getActiveUserComments(userId: UserId): F[Vector[CustomComment]]
  def getAllPostComments(postId: PostId): F[Vector[CustomComment]]
  def deleteAllPostComments(postId: PostId): F[Unit]
}
