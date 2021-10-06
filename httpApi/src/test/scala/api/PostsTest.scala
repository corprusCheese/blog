package api

import api.suites.HttpSuite
import blog.domain.posts.{CreatePost, Post}
import blog.domain.users.{User, UserCreate}
import blog.routes.Posts
import blog.storage.{CommentStorageDsl, PostStorageDsl, TagStorageDsl, UserStorageDsl}
import cats.effect.{IO, Resource}
import gen.generators._
import impl.{TestPostStorage, TestUserStorage}
import org.http4s.Method._
import org.http4s.Status.Ok
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._

object PostsTest extends HttpSuite {

  /*
  test("posts with pagination") {
    val gen = for {
      u <- userGen
      p <- postGen
      t <- tagGen
    } yield (u, p, t)

    forall(gen) {
      case (user, post, tag) =>
        val userStorage: Resource[IO, UserStorageDsl[IO]] =
          TestUserStorage.resource[IO]
        val postStorage: Resource[IO, PostStorageDsl[IO]] =
          TestPostStorage.resource[IO]
        val commentStorage: Resource[IO, CommentStorageDsl[IO]] = ???
        val tagStorage: Resource[IO, TagStorageDsl[IO]] = ???
        val testMiddleware: AuthMiddleware[IO, User] = ???

        userStorage.use(us =>
          postStorage.use(ps =>
            commentStorage.use(cs =>
              tagStorage.use(ts =>
                for {
                  _ <- us.create(
                    UserCreate(user.uuid, user.username, user.password)
                  )
                  _ <-
                    ps.create(CreatePost(post.postId, post.message, user.uuid))
                  routes = Posts[IO](ps, cs, ts).routes(testMiddleware)
                  all = GET(uri"/all")
                  expectAll <- expectHttpBodyAndStatus(routes, all)(
                    Vector(Post(post.postId, post.message, user.uuid)),
                    Ok
                  )
                  postId = post.postId
                  find = GET(uri"/{$postId}")
                  expectFind <- expectHttpBodyAndStatus(routes, find)(
                    Post(post.postId, post.message, user.uuid),
                    Ok
                  )
                } yield expectAll && expectFind
              )
            )
          )
        )
    }
  }
   */

}
