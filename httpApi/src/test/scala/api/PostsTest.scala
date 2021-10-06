package api

import api.suites.HttpSuite
import blog.domain._
import blog.domain.users.UserCreate
import blog.routes.Posts
import blog.storage._
import cats.effect.{IO, Resource}
import cats.implicits.catsSyntaxApplicativeId
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import ext.routes.helper._
import impl._
import org.http4s.Method.GET
import org.http4s.Status.{NotFound, Ok}
import org.http4s.Uri
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._
import weaver.Expectations

import java.util.UUID

object PostsTest extends HttpSuite {

  override type Res = (
      UserStorageDsl[IO],
      PostStorageDsl[IO],
      CommentStorageDsl[F],
      TagStorageDsl[F]
  )

  override def sharedResource: Resource[IO, Res] =
    for {
      us <- TestUserStorage.resource[IO]
      ps <- TestPostStorage.resource[IO]
      cs <- TestCommentStorage.resource[IO]
      ts <- TestTagStorage.resource[IO]
    } yield (us, ps, cs, ts)

  def testPagination(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      e <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(uri"/post/all")
      )(NotFound)
      _ <- createPost(ps)
      _ <- createPost(ps)
      _ <- createPost(ps)
      e1 <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(uri"/post/all")
      )(Ok)
      e2 <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(uri"/post/all?page=1")
      )(NotFound)
      _ <- createPost(ps)
      e3 <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(uri"/post/all?page=1")
      )(Ok)
    } yield e && e1 && e2 && e3

  def testGetById(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      uuidRandom <- UUID.randomUUID().pure[IO]
      e <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(Uri.unsafeFromString(s"/post/$uuidRandom"))
      )(NotFound)
      uuid <- createPost(ps)
      e1 <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(Uri.unsafeFromString(s"/post/$uuid"))
      )(Ok)
    } yield e && e1

  def testPostWithTags(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      tagUuid <- createTag(ts)
      _ <- createPost(ps, Vector(TagId(tagUuid)))
      e <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
        GET(Uri.unsafeFromString(s"/post/tag/$tagUuid"))
      )(Ok)
    } yield e

  def testPostWithUser(
      us: UserStorageDsl[IO],
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): IO[Expectations] =
    for {
      postUuid <- createPost(ps)
      opt <- ps.findById(PostId(postUuid))
      _ <- us.create(
        UserCreate(
          opt.get.userId,
          Username("asdsads"),
          HashedPassword("secret")
        )
      )
      e <- expectHttpStatus(
        Posts[IO](ps, cs, ts).routesWithoutAuthOnly,
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
