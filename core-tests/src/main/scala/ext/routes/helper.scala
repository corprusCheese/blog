package ext.routes

import blog.domain.posts._
import blog.domain.tags._
import blog.domain._
import blog.storage._
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import gen.generators._

import java.util.UUID

object helper {
  def createPost(
      ps: PostStorageDsl[IO],
      vec: Vector[TagId] = Vector.empty
  ): IO[UUID] =
    for {
      sample <- postGen.sample.pure[IO]
      _ <- ps.create(
        CreatePost(
          sample.get.postId,
          sample.get.message,
          sample.get.userId,
          vec
        )
      )
    } yield sample.get.postId.value

  def createTag(
      ts: TagStorageDsl[IO],
      vec: Vector[PostId] = Vector.empty
  ): IO[UUID] =
    for {
      sample <- tagGen.sample.pure[IO]
      _ <- ts.create(
        TagCreate(
          sample.get.tagId,
          sample.get.name,
          vec
        )
      )
    } yield sample.get.tagId.value
}
