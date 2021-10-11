package blog.programs

import blog.domain._
import blog.domain.posts._
import blog.errors._
import blog.storage._
import cats.MonadThrow
import cats.data.NonEmptyVector
import cats.implicits._
import org.http4s.circe.JsonDecoder

import java.util.UUID

trait PostProgram[F[_]] {
  def create(
      message: PostMessage,
      tagIds: Vector[TagId],
      userId: UserId
  ): F[Unit]
  def update(
      postId: PostId,
      message: PostMessage,
      tagIds: Vector[TagId],
      userId: UserId
  ): F[Unit]
  def delete(postId: PostId, userId: UserId): F[Unit]
  def paginatePosts(page: Page): F[Vector[Post]]
  def paginatePostsByTag(page: Page, tagId: TagId): F[Vector[Post]]
  def findById(postId: PostId): F[Option[Post]]
  def findByUser(userId: UserId): F[Vector[Post]]
}

object PostProgram {
  def make[F[_]: JsonDecoder: MonadThrow](
      postStorage: PostStorageDsl[F],
      commentStorage: CommentStorageDsl[F],
      tagStorage: TagStorageDsl[F]
  ): PostProgram[F] =
    new PostProgram[F] {
      override def create(
          message: PostMessage,
          tagIds: Vector[TagId],
          userId: UserId
      ): F[Unit] =
        checkIfTagIdsExisting(tagIds).flatMap {
          case false => throw PostTagsAreNotExisting
          case true =>
            postStorage
              .create(
                CreatePost(
                  PostId(UUID.randomUUID()),
                  message,
                  userId,
                  tagIds
                )
              )
              .handleErrorWith(_ => throw CreatePostError)
        }

      override def update(
          postId: PostId,
          message: PostMessage,
          tagIds: Vector[TagId],
          userId: UserId
      ): F[Unit] =
        checkIfTagIdsExisting(tagIds).flatMap {
          case false => throw PostTagsAreNotExisting
          case true =>
            postBelongsToUser(userId, postId).flatMap {
              case None => throw PostDontBelongToUser
              case _ =>
                postStorage
                  .update(
                    UpdatePost(
                      postId,
                      message,
                      tagIds
                    )
                  )
                  .handleErrorWith(_ => throw UpdatePostError)
            }
        }

      override def delete(postId: PostId, userId: UserId): F[Unit] =
        postBelongsToUser(userId, postId).flatMap {
          case None => throw PostNotExists
          case _ =>
            commentStorage
              .deleteAllPostComments(postId)
              .handleErrorWith(_ => throw DeletePostCommentError)
              .flatTap(_ =>
                postStorage
                  .delete(
                    DeletePost(postId)
                  )
              )
              .handleErrorWith(_ => throw DeletePostError)
        }

      override def paginatePosts(page: Page): F[Vector[Post]] =
        postStorage
          .fetchForPagination(page)

      override def findById(postId: PostId): F[Option[Post]] =
        postStorage
          .findById(postId)

      override def findByUser(userId: UserId): F[Vector[Post]] =
        postStorage
          .getAllUserPosts(userId)

      override def paginatePostsByTag(
          page: Page,
          tagId: TagId
      ): F[Vector[Post]] =
        postStorage
          .fetchPostForPaginationWithTags(
            NonEmptyVector.one(tagId),
            page
          )

      private def postBelongsToUser(
          userId: UserId,
          postId: PostId
      ): F[Option[Post]] =
        postStorage.findById(postId).map {
          case None => none[Post]
          case Some(post) =>
            if (post.userId == userId) post.some else none[Post]
        }

      private def checkIfTagIdsExisting(tagIds: Vector[TagId]): F[Boolean] =
        tagIds match {
          case v if v == Vector.empty => true.pure[F]
          case v if v != Vector.empty =>
            tagStorage.fetchAll
              .map(_.map(_.tagId).intersect(tagIds) == tagIds)
        }
    }
}
