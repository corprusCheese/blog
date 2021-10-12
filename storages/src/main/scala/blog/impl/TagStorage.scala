package blog.impl

import blog.domain._
import blog.domain.tags._
import blog.meta._
import blog.queries.tagQueries
import blog.queries.tagQueries._
import blog.storage.TagStorageDsl
import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits._
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor
import doobie.util.transactor.Transactor
import doobie.{ConnectionIO, Fragments, Update}
import org.typelevel.log4cats.Logger

case class TagStorage[F[_]: Logger: MonadCancelThrow](tx: Transactor[F])
    extends TagStorageDsl[F] {

  override def findById(id: TagId): F[Option[Tag]] =
    queryForFindById(id).option
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding tag by id = ${id}"))
      .map(_.map {
        case (tagId, tagName) =>
          Tag(tagId, tagName)
      })

  override def fetchAll: F[Vector[Tag]] =
    queryForFetchAll
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding all tags"))
      .map(_.map {
        case (tagId, tagName) =>
          Tag(tagId, tagName)
      })

  override def findByName(name: TagName): F[Vector[Tag]] =
    queryForFindByName(name)
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding tags by name = ${name}"))
      .map(_.map {
        case (tagId, tagName) =>
          Tag(tagId, tagName)
      })

  override def getAllPostTags(postId: PostId): F[Vector[Tag]] =
    queryForGetPostTags(postId)
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding tags by post_id = ${postId}"))
      .map(_.map {
        case (tagId, tagName) =>
          Tag(tagId, tagName)
      })

  override def create(create: TagCreate): F[Unit] =
    queryForCreateTag(create).run
      .transact(tx)
      .flatTap(_ =>
        addPostsToTag(create.tagId, create.postsId) >> Logger[F]
          .info("creating new tag")
      )
      .map(_ => ())

  override def update(update: TagUpdate): F[Unit] =
    queryForUpdateTag(update).run
      .transact(tx)
      .flatTap(_ =>
        updatePostsOfTag(update.tagId, update.postsId) >>
          Logger[F].info(s"updating tag with id = ${update.tagId}")
      )
      .map(_ => ())

  override def delete(delete: TagDelete): F[Unit] =
    queryForDeleteTag(delete).run
      .transact(tx)
      .flatTap(_ =>
        deleteTagFromPosts(delete.tagId) >>
          Logger[F].info(s"deleting tag with id = ${delete.tagId}")
      )
      .map(_ => ())

  private def addPostsToTag(tagId: TagId, postIds: Vector[PostId]): F[Unit] =
    Update[(PostId, TagId)](
      "INSERT INTO posts_tags (post_id, tag_id) VALUES (?,?)"
    ).updateMany(postIds.map((_, tagId)))
      .transact(tx)
      .map(_ => ())

  private def deleteTagFromPosts(tagId: TagId): F[Unit] =
    queryForDeleteTagFromPosts(tagId).run
      .transact(tx)
      .map(_ => ())

  private def updatePostsOfTag(
      tagId: TagId,
      postsIds: Vector[PostId]
  ): F[Unit] = deleteTagFromPosts(tagId) >> addPostsToTag(tagId, postsIds)
}

object TagStorage {
  def resource[F[_]: Logger: MonadCancelThrow](
      tx: transactor.Transactor[F]
  ): Resource[F, TagStorage[F]] =
    Resource.pure(TagStorage[F](tx))
}
