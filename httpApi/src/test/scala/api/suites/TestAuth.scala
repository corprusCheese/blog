package api.suites

import blog.auth.{AuthCommands, TokenManager}
import blog.config.{JwtAccessTokenKey, TokenExpiration}
import blog.impl.AuthCache
import blog.storage._
import cats.effect.{IO, Resource}
import eu.timepit.refined.types.string.NonEmptyString
import impl._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger

import scala.concurrent.duration.DurationInt

abstract class TestAuth extends HttpSuite {

  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO] // todo: take care of logger

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
      tokenExpiration = TokenExpiration(10.seconds)
      key = JwtAccessTokenKey.apply(NonEmptyString("secret test key"))
      tokenManager <- TokenManager.resource(tokenExpiration, key)
      ac = AuthCommands.make(authCache, us, tokenManager, tokenExpiration)
    } yield (us, ps, cs, ts, authCache, ac)
}
