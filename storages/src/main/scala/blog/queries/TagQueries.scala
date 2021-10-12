package blog.queries

import blog.domain.tags.{TagCreate, TagDelete, TagUpdate}
import blog.domain.{PostId, TagId, TagName}
import blog.meta._
import doobie.Fragments
import doobie.implicits.toSqlInterpolator

object TagQueries {
  def queryForFindById[F[_]](id: TagId): doobie.Query0[(TagId, TagName)] =
    sql"""SELECT uuid, name FROM tags WHERE uuid = $id AND deleted = false"""
      .query[(TagId, TagName)]

  def queryForFetchAll[F[_]]: doobie.Query0[(TagId, TagName)] =
    sql"""SELECT uuid, name FROM tags WHERE deleted = false"""
      .query[(TagId, TagName)]

  def queryForFindByName[F[_]](name: TagName): doobie.Query0[(TagId, TagName)] =
    sql"""SELECT uuid, name FROM tags WHERE name = ${name} AND deleted = false"""
      .query[(TagId, TagName)]

  def queryForGetPostTags[F[_]](postId: PostId): doobie.Query0[(TagId, TagName)] =
    sql"""SELECT uuid, name FROM tags JOIN posts_tags ON tags.uuid = posts_tags.tag_id WHERE post_id = ${postId} AND deleted = false"""
      .query[(TagId, TagName)]

  def queryForUpdateBoundTable[F[_]](tagId: TagId, postId: PostId): doobie.Update0 =
    sql"""INSERT INTO posts_tags (post_id, tag_id) VALUES ($postId, $tagId)""".update

  def queryForDeleteTagFromPosts[F[_]](tagId: TagId): doobie.Update0 =
    sql"""DELETE FROM posts_tags WHERE tag_id = $tagId""".update

  def queryForCreateTag[F[_]](create: TagCreate): doobie.Update0 =
    sql"""INSERT INTO tags (uuid, name) VALUES (${create.tagId}, ${create.name})""".update

  def queryForDeleteTag[F[_]](delete: TagDelete): doobie.Update0 =
    sql"""UPDATE tags SET deleted = true WHERE uuid = ${delete.tagId}""".update

  def queryForUpdateTag[F[_]](update: TagUpdate): doobie.Update0 =
    sql"""UPDATE tags SET name = ${update.name} WHERE uuid = ${update.tagId}""".update
}
