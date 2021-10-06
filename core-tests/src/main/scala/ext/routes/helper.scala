package ext.routes

import blog.domain.posts._
import blog.domain.tags._
import blog.domain._
import blog.storage._
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._

import java.util.UUID

object helper {
  def createPost(
      ps: PostStorageDsl[IO],
      vec: Vector[TagId] = Vector.empty
  ): IO[UUID] =
    for {
      uuid <- UUID.randomUUID().pure[IO]
      _ <- ps.create(
        CreatePost(
          PostId(uuid),
          PostMessage("asdsads"),
          UserId(uuid),
          vec
        )
      )
    } yield uuid

  def createTag(
      ts: TagStorageDsl[IO],
      vec: Vector[PostId] = Vector.empty
  ): IO[UUID] =
    for {
      uuid <- UUID.randomUUID().pure[IO]
      _ <- ts.create(
        TagCreate(
          TagId(uuid),
          TagName("asdsads"),
          vec
        )
      )
    } yield uuid
}
