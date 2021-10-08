package api.suites

import api.post.PostsTest.{POST_AUTH, expectHttpStatus}
import blog.auth.{AuthCommands, TokenManager}
import blog.config.{JwtAccessTokenKey, TokenExpiration}
import blog.domain.users.User
import blog.storage._
import cats.effect.{IO, Resource}
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import impl._
import org.http4s.Method.POST
import org.http4s.Status.{Forbidden, UnprocessableEntity}
import org.http4s.client.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.{AuthScheme, Credentials, HttpRoutes, Request, Uri}
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

import scala.concurrent.duration.DurationInt

abstract class TestAuth extends HttpSuite {

  implicit val logger: SelfAwareStructuredLogger[IO] =
    NoOpLogger[IO] // todo: take care of logger

  type Storages = (
      UserStorageDsl[IO],
      PostStorageDsl[IO],
      CommentStorageDsl[IO],
      TagStorageDsl[IO],
      AuthCacheDsl[IO],
      AuthCommands[IO]
  )

  def resourceStorages: Resource[IO, Storages] =
    for {
      us <- TestUserStorage.resource[IO]
      ps <- TestPostStorage.resource[IO]
      cs <- TestCommentStorage.resource[IO]
      ts <- TestTagStorage.resource[IO]
      authCache <- TestAuthCache.resource[IO]
      tokenExpiration = TokenExpiration(30.minutes)
      key = JwtAccessTokenKey.apply(NonEmptyString("secret"))
      tokenManager <- TokenManager.resource(tokenExpiration, key)
      ac = AuthCommands.make(authCache, us, tokenManager, tokenExpiration)
    } yield (us, ps, cs, ts, authCache, ac)

  def POST_AUTH(uri: Uri, token: JwtToken): Request[IO] =
    POST(
      uri,
      Authorization(
        Credentials.Token(AuthScheme.Bearer, token.value)
      )
    )

  def firstStep(
      uri: Uri,
      routes: HttpRoutes[IO],
      user: User,
      ac: AuthCommands[IO]
  ): IO[(Boolean, Boolean, JwtToken)] =
    for {
      /* check route with auth */
      withoutAuth <- expectHttpStatus(
        routes,
        POST(uri)
      )(Forbidden)
      /* creating token and try to auth with empty body */
      token <- ac.newUser(user.userId, user.username, user.password).map(_.get)
      withAuthWrongBody <- expectHttpStatus(
        routes,
        POST_AUTH(uri, token)
      )(UnprocessableEntity)
    } yield (withoutAuth, withAuthWrongBody, token)

}
