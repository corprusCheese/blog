package api.get

import api.suites.TestCommon
import blog.domain._
import blog.programs.TagProgram
import blog.routes._
import blog.storage._
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import ext.routes.helper._
import gen.generators.tagGen
import org.http4s.Method.GET
import org.http4s.Status.{NotFound, Ok}
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._
import org.http4s.{HttpRoutes, Uri}

import java.util.UUID

/**
  * test without forall using
  */
object TagsTest extends TestCommon {

  private def routesForTesting(
      ts: TagStorageDsl[IO],
      ps: PostStorageDsl[IO]
  ): HttpRoutes[IO] = Tags[IO](TagProgram.make(ts, ps)).routesWithoutAuthOnly

  test("fetch all") {
    resourceStorages.use {
      case (_, ps, _, ts) =>
        for {
          noTags <- expectHttpStatus(
            routesForTesting(ts, ps),
            GET(uri"/tag/all")
          )(NotFound)
          _ <- createTag(ts)
          _ <- createTag(ts)
          expectedBody <- ts.fetchAll
          expected2Tags <- expectHttpBodyAndStatus(
            routesForTesting(ts, ps),
            GET(uri"/tag/all")
          )(expectedBody, Ok)
        } yield expect.all(noTags, expected2Tags)
    }
  }

  test("get by id or name") {
    resourceStorages.use {
      case (_, ps, _, ts) =>
        for {
          sample <- tagGen.sample.pure[IO]
          noId <- expectHttpStatus(
            routesForTesting(ts, ps),
            GET(Uri.unsafeFromString(s"/tag/id/${sample.get.tagId}"))
          )(NotFound)
          uuid <- createTag(ts)
          getBody <- ts.findById(TagId(uuid))
          existId <- expectHttpBodyAndStatus(
            routesForTesting(ts, ps),
            GET(Uri.unsafeFromString(s"/tag/id/${uuid}"))
          )(getBody.get, Ok)
          existName <- expectHttpBodyAndStatus(
            routesForTesting(ts, ps),
            GET(Uri.unsafeFromString(s"/tag/name/${getBody.get.name}"))
          )(Vector(getBody.get), Ok)
        } yield expect.all(noId, existId, existName)
    }
  }

  test("get tags from post") {
    resourceStorages.use {
      case (_, ps, _, ts) =>
        for {
          noPost <- expectHttpStatus(
            routesForTesting(ts, ps),
            GET(Uri.unsafeFromString(s"/tag/post/${UUID.randomUUID()}"))
          )(NotFound)
          postUuid <- createPost(ps)
          _ <- createTag(ts, Vector(PostId(postUuid)))
          getBody <- ts.getAllPostTags(PostId(postUuid))
          postExistsAndTagToo <- expectHttpBodyAndStatus(
            routesForTesting(ts, ps),
            GET(Uri.unsafeFromString(s"/tag/post/$postUuid"))
          )(getBody, Ok)
        } yield expect.all(noPost, postExistsAndTagToo)
    }
  }
}
