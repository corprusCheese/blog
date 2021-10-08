package api.get

import api.suites.TestCommon
import blog.domain._
import blog.domain.users._
import blog.programs.PostProgram
import blog.routes._
import blog.storage._
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import ext.routes.helper._
import gen.generators.userGen
import org.http4s.Method.GET
import org.http4s.Status.{NotFound, Ok}
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._
import org.http4s.{HttpRoutes, Uri}

import java.util.UUID

/**
  * test without forall using
  */
object PostsTest extends TestCommon {

  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): HttpRoutes[IO] =
    Posts[IO](PostProgram.make(ps, cs, ts)).routesWithoutAuthOnly

  test("pagination") {
    resourceStorages.use {
      case (_, ps, cs, ts) =>
        for {
          emptyStorage <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(uri"/post/all")
          )(NotFound)
          _ <- createPost(ps)
          _ <- createPost(ps)
          _ <- createPost(ps)
          expectedBody <- ps.fetchForPagination(0)
          page0 <- expectHttpBodyAndStatus(
            routesForTesting(ps, cs, ts),
            GET(uri"/post/all")
          )(expectedBody, Ok)
          page1When3Items <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(uri"/post/all?page=1")
          )(NotFound)
          _ <- createPost(ps)
          page1When4Items <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(uri"/post/all?page=1")
          )(Ok)
        } yield expect.all(
          emptyStorage,
          page0,
          page1When3Items,
          page1When4Items
        )
    }
  }

  test("get by id") {
    resourceStorages.use {
      case (_, ps, cs, ts) =>
        for {
          uuidRandom <- UUID.randomUUID().pure[IO]
          notExistingId <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(Uri.unsafeFromString(s"/post/$uuidRandom"))
          )(NotFound)
          uuid <- createPost(ps)
          existingId <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(Uri.unsafeFromString(s"/post/$uuid"))
          )(Ok)
        } yield expect.all(notExistingId, existingId)
    }
  }

  test("post with tags") {
    resourceStorages.use {
      case (_, ps, cs, ts) =>
        for {
          tagUuid <- createTag(ts)
          _ <- createPost(ps, Vector(TagId(tagUuid)))
          existingId <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(Uri.unsafeFromString(s"/post/tag/$tagUuid"))
          )(Ok)
        } yield expect.all(existingId)
    }
  }

  test("post with user") {
    resourceStorages.use {
      case (us, ps, cs, ts) =>
        for {
          sample <- userGen.sample.pure[IO]
          postUuid <- createPost(ps)
          opt <- ps.findById(PostId(postUuid))
          _ <- us.create(
            UserCreate(
              opt.get.userId,
              sample.get.username,
              sample.get.password
            )
          )
          existingId <- expectHttpStatus(
            routesForTesting(ps, cs, ts),
            GET(Uri.unsafeFromString(s"/post/user/${opt.get.userId}"))
          )(Ok)
        } yield expect.all(existingId)
    }
  }
}
