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
import org.http4s.{HttpRoutes, Uri}
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._
import weaver.Expectations

import java.util.UUID

object PostsTest extends TestCommon {

  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): HttpRoutes[IO] = Posts[IO](PostProgram.make(ps, cs, ts)).routesWithoutAuthOnly

  private def testPagination(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      e <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(uri"/post/all")
      )(NotFound)
      _ <- createPost(ps)
      _ <- createPost(ps)
      _ <- createPost(ps)
      expectedBody <- ps.fetchForPagination(0)
      e1 <- expectHttpBodyAndStatus(
        routesForTesting(ps, cs, ts),
        GET(uri"/post/all")
      )(expectedBody, Ok)
      e2 <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(uri"/post/all?page=1")
      )(NotFound)
      _ <- createPost(ps)
      e3 <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(uri"/post/all?page=1")
      )(Ok)
    } yield e && e1 && e2 && e3

  private def testGetById(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      uuidRandom <- UUID.randomUUID().pure[IO]
      e <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(Uri.unsafeFromString(s"/post/$uuidRandom"))
      )(NotFound)
      uuid <- createPost(ps)
      e1 <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(Uri.unsafeFromString(s"/post/$uuid"))
      )(Ok)
    } yield e && e1

  private def testPostWithTags(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      tagUuid <- createTag(ts)
      _ <- createPost(ps, Vector(TagId(tagUuid)))
      e <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(Uri.unsafeFromString(s"/post/tag/$tagUuid"))
      )(Ok)
    } yield e

  private def testPostWithUser(
      us: UserStorageDsl[IO],
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
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
      e <- expectHttpStatus(
        routesForTesting(ps, cs, ts),
        GET(Uri.unsafeFromString(s"/post/user/${opt.get.userId}"))
      )(Ok)
    } yield e

  test("all routes healthcare") {
    _ match {
      case (us, ps, cs, ts) =>
        for {
          pagination <- testPagination(ps, cs, ts)
          getById <- testGetById(ps, cs, ts)
          postWithTags <- testPostWithTags(ps, cs, ts)
          postWithUser <- testPostWithUser(us, ps, cs, ts)
        } yield pagination && getById && postWithTags && postWithUser
    }
  }
}
