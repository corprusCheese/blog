package blog.impl

import blog.domain._
import blog.domain.tags._
import blog.domain.users._
import blog.storage.TagStorageDsl
import cats.effect.{MonadCancelThrow, Resource}
import doobie.util.transactor
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger
import blog.meta._
import doobie.implicits._
import cats.implicits._

case class TagStorage[F[_]: Logger: MonadCancelThrow](tx: Transactor[F])
    extends TagStorageDsl[F] {
  override def findById(id: TagId): F[Option[Tag]] =
    sql"""SELECT uuid, name, deleted FROM tags WHERE uuid = ${id}"""
      .query[(TagId, TagName, Deleted)]
      .option
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding tag by id = ${id}").pure[F])
      .map(_.map {
        case (tagId, tagName, deleted) =>
          Tag(tagId, tagName, deleted)
      })

  override def fetchAll: F[Vector[Tag]] =
    sql"""SELECT uuid, name, deleted FROM tags WHERE deleted = false"""
      .query[(TagId, TagName, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding all tags").pure[F])
      .map(_.map {
        case (tagId, tagName, deleted) =>
          Tag(tagId, tagName, deleted)
      })

  override def findByName(name: TagName): F[Vector[Tag]] =
    sql"""SELECT uuid, name, deleted FROM tags where name = ${name}"""
      .query[(TagId, TagName, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding tags by name = ${name}").pure[F])
      .map(_.map {
        case (tagId, tagName, deleted) =>
          Tag(tagId, tagName, deleted)
      })

  override def getAllPostTags(postId: PostId): F[Vector[Tag]] =
    sql"""SELECT uuid, name, deleted FROM tags JOIN posts_tags ON tags.uuid = posts_tags.tag_id WHERE post_id = ${postId} AND deleted = false"""
      .query[(TagId, TagName, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding tags by post_id = ${postId}").pure[F])
      .map(_.map {
        case (tagId, tagName, deleted) =>
          Tag(tagId, tagName, deleted)
      })

  override def create(create: TagCreate): F[Unit] =
    sql"""INSERT INTO tags (uuid, name) VALUES (${create.tagId}, ${create.name})""".update.run
      .transact(tx)
      .flatTap(_ => Logger[F].info("creating new user").pure[F])
      .map(_ => ())

  override def update(update: TagUpdate): F[Unit] =
    sql"""UPDATE tags SET name = ${update.name} WHERE uuid = ${update.tagId}""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"updating tag with id = ${update.tagId}").pure[F]
      )
      .map(_ => ())

  override def delete(delete: TagDelete): F[Unit] =
    sql"""UPDATE tags SET deleted = true WHERE uuid = ${delete.tagId}""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"deleting tag with id = ${delete.tagId}").pure[F]
      )
      .map(_ => ())
}

object TagStorage {
  def resource[F[_]: Logger: MonadCancelThrow](
      tx: transactor.Transactor[F]
  ): Resource[F, TagStorageDsl[F]] =
    Resource.pure[F, TagStorage[F]](TagStorage[F](tx))
}
