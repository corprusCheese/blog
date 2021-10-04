package blog.impl

import blog.config.types.PaginationOptions
import blog.domain._
import blog.domain.posts._
import blog.storage.PostStorageDsl
import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits._
import doobie.implicits._
import doobie.util.transactor
import doobie.util.transactor.Transactor
import doobie.refined._
import doobie.refined.implicits._
import eu.timepit.refined.auto._
import org.typelevel.log4cats.Logger
import blog.meta._
import cats.data.NonEmptyVector
import doobie.Update
import doobie.util.fragments.whereAndOpt
import doobie.Fragments

case class PostStorage[F[_]: Logger: MonadCancelThrow](
    tx: Transactor[F],
    paginationOptions: PaginationOptions
) extends PostStorageDsl[F] {

  private val perPage: PerPage = paginationOptions.perPage

  override def findById(id: PostId): F[Option[Post]] =
    sql"""SELECT uuid, posts.message, user_id, deleted FROM posts WHERE uuid = ${id}"""
      .query[(PostId, PostMessage, UserId, Deleted)]
      .option
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding post by id = ${id}"))
      .map(_.map {
        case (postId, message, userId, deleted) =>
          Post(postId, message, userId, deleted)
      })

  override def fetchForPagination(page: Page): F[Vector[Post]] =
    sql"""SELECT uuid, posts.message, user_id, deleted FROM posts WHERE deleted = false LIMIT ${perPage} OFFSET ${page * perPage}"""
      .query[(PostId, PostMessage, UserId, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"fetching next ${perPage} for page ${page}")
      )
      .map(_.map {
        case (postId, message, userId, deleted) =>
          Post(postId, message, userId, deleted)
      })

  override def getAllUserPosts(userId: UserId): F[Vector[Post]] =
    sql"""SELECT uuid, posts.message, user_id, deleted FROM posts WHERE user_id = ${userId} AND deleted = false"""
      .query[(PostId, PostMessage, UserId, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"fetching all post of user id = ${userId}"))
      .map(_.map {
        case (postId, message, userId, deleted) =>
          Post(postId, message, userId, deleted)
      })

  override def fetchPostForPaginationWithTags(
      tagIds: NonEmptyVector[TagId],
      page: Page
  ): F[Vector[Post]] =
    (fr"SELECT uuid, posts.message, user_id, deleted FROM posts JOIN posts_tags ON posts.uuid = posts_tags.post_id WHERE " ++ Fragments
      .in(
        fr"posts_tags.tag_id",
        tagIds
      ) ++ fr" LIMIT ${perPage} OFFSET ${page * perPage}")
      .query[(PostId, PostMessage, UserId, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"fetching next ${perPage} for page ${page}")
      )
      .map(_.map {
        case (postId, message, userId, deleted) =>
          Post(postId, message, userId, deleted)
      })

  override def create(create: CreatePost): F[Unit] =
    sql"""INSERT INTO posts (uuid, message, user_id) VALUES (${create.postId}, ${create.message}, ${create.userId})""".update.run
      .transact(tx)
      .flatMap(_ =>
        addTagsToPost(create.postId, create.tagsId) >> Logger[F]
          .info("creating new post")
      )
      .map(_ => ())

  override def update(update: UpdatePost): F[Unit] =
    sql"""UPDATE posts SET message = ${update.message} WHERE uuid = ${update.postId}""".update.run
      .transact(tx)
      .flatMap(_ =>
        updateTagsOfPost(update.postId, update.tagsId) >> Logger[F]
          .info(s"updating post with id = ${update.postId}")
      )
      .map(_ => ())

  override def delete(delete: DeletePost): F[Unit] =
    sql"""UPDATE posts SET deleted = true WHERE uuid = ${delete.postId}""".update.run
      .transact(tx)
      .flatMap(_ =>
        deletePostFromTags(delete.postId) >> Logger[F]
          .info(s"deleting post with id = ${delete.postId}")
      )
      .map(_ => ())

  private def addTagsToPost(postId: PostId, tagIds: Vector[TagId]): F[Unit] =
    Update[(PostId, TagId)](
      "INSERT INTO posts_tags (post_id, tag_id) VALUES (?,?)"
    ).updateMany(tagIds.map((postId, _)))
      .transact(tx)
      .map(_ => ())

  private def deletePostFromTags(postId: PostId): F[Unit] =
    sql"""DELETE FROM posts_tags WHERE post_id = ${postId}""".update.run
      .transact(tx)
      .map(_ => ())

  private def updateTagsOfPost(postId: PostId, tagIds: Vector[TagId]): F[Unit] =
    deletePostFromTags(postId) >> addTagsToPost(postId, tagIds)

}

object PostStorage {
  def resource[F[_]: Logger: MonadCancelThrow](
      tx: transactor.Transactor[F],
      paginationOptions: PaginationOptions
  ): Resource[F, PostStorageDsl[F]] =
    Resource.pure[F, PostStorage[F]](PostStorage[F](tx, paginationOptions))
}
