package blog.programs

import blog.domain._
import blog.domain.posts.Post
import blog.domain.tags._
import blog.errors._
import blog.storage.{PostStorageDsl, TagStorageDsl}
import cats.{Monad, MonadThrow}
import cats.implicits._
import org.http4s.circe.JsonDecoder

import java.util.UUID

trait TagProgram[F[_]] {
  def create(tagName: TagName, postIds: Vector[PostId]): F[Unit]
  def update(
      tagId: TagId,
      tagName: TagName,
      postIds: Vector[PostId],
      userId: UserId
  ): F[Unit]
  def delete(tagId: TagId, userId: UserId): F[Unit]
  def getAll: F[Vector[Tag]]
  def findById(tagId: TagId): F[Option[Tag]]
  def findByName(tagName: TagName): F[Vector[Tag]]
  def getPostTags(postId: PostId): F[Vector[Tag]]
}

object TagProgram {
  def make[F[_]: JsonDecoder: MonadThrow](
      tagStorage: TagStorageDsl[F],
      postStorage: PostStorageDsl[F]
  ): TagProgram[F] =
    new TagProgram[F] {
      override def create(
          tagName: TagName,
          postIds: Vector[PostId]
      ): F[Unit] = {
        checkIfPostIdsExisting(postIds).flatMap {
          case false => throw TagPostsAreNotExisting
          case true =>
            tagStorage
              .create(
                TagCreate(
                  TagId(UUID.randomUUID()),
                  tagName,
                  postIds
                )
              )
              .handleErrorWith(_ => throw CreateTagError)
        }
      }

      override def update(
          tagId: TagId,
          tagName: TagName,
          postIds: Vector[PostId],
          userId: UserId
      ): F[Unit] =
        canUserUpdateAllRequestedPosts(userId, postIds)
          .flatMap {
            case false => throw UpdateTagWithNotYoursPosts
            case true =>
              tagStorage
                .update(
                  TagUpdate(
                    tagId,
                    tagName,
                    postIds
                  )
                )
                .handleErrorWith(_ => throw UpdateTagError)
          }

      override def delete(tagId: TagId, userId: UserId): F[Unit] = ???

      override def getAll: F[Vector[Tag]] = tagStorage.fetchAll

      override def findById(tagId: TagId): F[Option[Tag]] =
        tagStorage.findById(tagId)

      override def findByName(tagName: TagName): F[Vector[Tag]] =
        tagStorage.findByName(tagName)

      override def getPostTags(postId: PostId): F[Vector[Tag]] =
        tagStorage.getAllPostTags(postId)

      private def canUserUpdateAllRequestedPosts(
          userId: UserId,
          postIds: Vector[PostId]
      ): F[Boolean] =
        postStorage
          .getAllUserPosts(userId)
          .map(_.map(_.postId).intersect(postIds))
          .map {
            case v if v.isEmpty && postIds.nonEmpty => false
            case v                                  => v == postIds
          }

      private def checkIfPostIdsExisting(postIds: Vector[PostId]): F[Boolean] =
        postIds match {
          case v if v == Vector.empty => true.pure[F]
          case v if v != Vector.empty =>
            // todo: method that check such things in db
            postIds
              .map(postStorage.findById)
              .sequence
              .map(!_.exists(_ == none[Post]))
        }
    }
}
