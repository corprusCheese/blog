package impl

import blog.domain._
import blog.domain.posts._
import blog.storage.PostStorageDsl
import cats._
import cats.data.NonEmptyVector
import cats.effect.{Ref, Resource}
import cats.implicits._
import eu.timepit.refined.auto._
import impl.helper.PostTagsStorage

case class TestPostStorage[F[_]: Monad](
    postTagsStorage: PostTagsStorage[F],
    inMemoryVector: Ref[F, Vector[Post]]
) extends PostStorageDsl[F] {

  private val perPage: PerPage = 10

  override def findById(id: PostId): F[Option[Post]] =
    inMemoryVector.get.map(_.find(_.postId == id))

  override def fetchForPagination(page: Page): F[Vector[Post]] =
    inMemoryVector.get.map(_.slice(page * perPage, (page + 1) * perPage))

  override def getAllUserPosts(userId: UserId): F[Vector[Post]] =
    inMemoryVector.get.map(_.filter(_.userId == userId))

  override def fetchPostForPaginationWithTags(
      tagIds: NonEmptyVector[TagId],
      page: Page
  ): F[Vector[Post]] = {
    val vecIds = tagIds.toVector
      .map(tagId => postTagsStorage.findByTagId(tagId))
      .sequence
      .map(_.flatten)

    vecIds.flatMap(v =>
      inMemoryVector.get.map(
        _.filter(post => v.contains(post.postId))
          .slice(page * perPage, (page + 1) * perPage)
      )
    )
  }

  override def delete(delete: DeletePost): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.filter(_.postId != delete.postId)
      _ <- inMemoryVector.set(newVector)
      tags <-
        postTagsStorage
          .findByPostId(delete.postId)
      _ <- tags.map(postTagsStorage.deleteTagId).sequence
    } yield ()

  override def create(create: CreatePost): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get :+ Post(create.postId, create.message, create.userId)
      _ <- inMemoryVector.set(newVector)
      _ <- postTagsStorage.create(create.postId, create.tagsId)
    } yield ()

  override def update(update: UpdatePost): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.map(post =>
        if (post.postId == update.postId)
          Post(update.postId, update.message, post.userId)
        else post
      )
      _ <- inMemoryVector.set(newVector)
      _ <- postTagsStorage.deletePostId(update.postId)
      _ <- postTagsStorage.create(update.postId, update.tagsId)
    } yield ()
}

object TestPostStorage {
  def resource[F[_]: Monad: Ref.Make]: Resource[F, PostStorageDsl[F]] =
    for {
      postTagStorage <- PostTagsStorage.resource[F]
      ref <- Resource.eval(
        Ref
          .of[F, Vector[Post]](Vector.empty)
          .map(inMemoryVector =>
            TestPostStorage[F](postTagStorage, inMemoryVector)
          )
      )
    } yield ref
}
