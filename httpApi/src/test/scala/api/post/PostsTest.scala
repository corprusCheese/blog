package api.post

import api.suites.TestAuth
import blog.config.JwtSecretKey
import blog.domain.requests.PostCreation
import blog.domain.tags.TagCreate
import blog.domain.users.User
import blog.middlewares.commonAuthMiddleware
import blog.programs.PostProgram
import blog.routes.Posts
import blog.storage._
import cats.data.NonEmptyVector
import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import gen.generators._
import org.http4s.Method.POST
import org.http4s.Status._
import org.http4s.client.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._
import org.http4s.{AuthScheme, Credentials, HttpRoutes}

object PostsTest extends TestAuth {
  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO],
      ac: AuthCacheDsl[IO]
  ): HttpRoutes[IO] = {
    val jwtAuth: JwtSecretKey = JwtSecretKey.apply(
      NonEmptyString.unsafeFrom("secret")
    )
    val authMiddleware: AuthMiddleware[F, User] =
      commonAuthMiddleware(ac, jwtAuth)
    Posts[IO](PostProgram.make(ps, cs, ts)).routesWithAuthOnly(authMiddleware)
  }

  test("create simple test") {
    val gen = for {
      u <- userGen
      p1 <- postGen
      p2 <- postGen
      t <- tagGen
    } yield (u, p1, p2, t)

    forall(gen) {
      case (user, post1, post2, tag) =>
        resourceStorages.use {
          case (us, ps, cs, ts, authCache, ac) =>
            for {
              /* check route with auth */
              withoutAuth <- expectHttpStatus(
                routesForTesting(ps, cs, ts, authCache),
                POST(uri"/post/create")
              )(Forbidden)
              /* creating token and try to auth */
              token <- ac.newUser(user.userId, user.username, user.password)
              withAuthWrongBody <- expectHttpStatus(
                routesForTesting(ps, cs, ts, authCache),
                POST(
                  uri"/post/create",
                  Authorization(
                    Credentials.Token(AuthScheme.Bearer, token.get.value)
                  )
                )
              )(UnprocessableEntity)
              /* use right format for query  */
              withAuthOkBody <- expectHttpStatusFromQuery(
                routesForTesting(ps, cs, ts, authCache),
                POST(
                  uri"/post/create",
                  Authorization(
                    Credentials.Token(AuthScheme.Bearer, token.get.value)
                  )
                ),
                post1
              )(Created)
              /* use wrong tag ids - it does not exist now */
              updatedPost2 = PostCreation(
                post2.message,
                NonEmptyVector.one(tag.tagId).some
              )
              notExistingTags <- expectHttpStatusFromQuery(
                routesForTesting(ps, cs, ts, authCache),
                POST(
                  uri"/post/create",
                  Authorization(
                    Credentials.Token(AuthScheme.Bearer, token.get.value)
                  )
                ),
                updatedPost2
              )(BadRequest)
              /* tag exists now - server should return Created */
              _ <- ts.create(TagCreate(tag.tagId, tag.name))
              existingTags <- expectHttpStatusFromQuery(
                routesForTesting(ps, cs, ts, authCache),
                POST(
                  uri"/post/create",
                  Authorization(
                    Credentials.Token(AuthScheme.Bearer, token.get.value)
                  )
                ),
                updatedPost2
              )(Created)
            } yield expect.all(
              withoutAuth,
              token.nonEmpty,
              withAuthWrongBody,
              withAuthOkBody,
              notExistingTags,
              existingTags
            )
        }
    }
  }
}
