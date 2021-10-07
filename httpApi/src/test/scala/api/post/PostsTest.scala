package api.post

import api.suites.TestAuth
import blog.config.JwtSecretKey
import blog.domain.users.User
import blog.middlewares.commonAuthMiddleware
import blog.programs.PostProgram
import blog.routes.Posts
import blog.storage.{AuthCacheDsl, CommentStorageDsl, PostStorageDsl, TagStorageDsl}
import cats.effect.IO
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import gen.generators._
import org.http4s.HttpRoutes
import org.http4s.Method.POST
import org.http4s.Status.{BadRequest, Forbidden, Ok, Unauthorized}
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._

object PostsTest extends TestAuth {
  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO],
      ac: AuthCacheDsl[IO]
  ): HttpRoutes[IO] = {
    val jwtAuth: JwtSecretKey = JwtSecretKey.apply(NonEmptyString.unsafeFrom("my very secret test password"))
    val authMiddleware: AuthMiddleware[F, User] = commonAuthMiddleware(ac, jwtAuth)
    Posts[IO](PostProgram.make(ps, cs, ts)).routesWithAuthOnly(authMiddleware)
  }

  test("create simple test") {
    val gen = for {
      u <- userGen
      p1 <- postGen
      p2 <- postGen
    } yield (u, p1, p2)

    forall(gen) {
      case (user, post1, post2) =>
        resourceStorages.use {
          case (us, ps, cs, ts, authCache, ac) =>
            for {
              e <- expectHttpStatus(
                routesForTesting(ps, cs, ts, authCache),
                POST(uri"/post/create")
              )(Forbidden)
              token <- ac.newUser(user.userId, user.username, user.password)
              e1 <- expectHttpStatus(
                routesForTesting(ps, cs, ts, authCache),
                POST(uri"/post/create")
              )(BadRequest) // todo: method that can send query with body and token
              /*e1 <- expectHttpBodyAndStatus(
                routesForTesting(ps, cs, ts, ac),
                POST(uri"/post/create")
              )(expectedBody, Ok)*/
            } yield expect.all(e, token.nonEmpty, e1)
        }
    }
  }
}
