package blog.impl

import blog.domain._
import blog.domain.users._
import blog.storage.AuthCacheDsl
import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import cats._
import eu.timepit.refined.auto._


import scala.concurrent.duration.FiniteDuration

case class AuthCache[F[_]: Monad](
    redis: RedisCommands[F, String, String]
) extends AuthCacheDsl[F] {
  override def getTokenAsString(userId: UserId): F[Option[String]] =
    redis.get(prefixUserId(userId))

  override def getUserAsString(token: JwtToken): F[Option[String]] =
    redis.get(prefixToken(token))

  override def setToken(
      user: User,
      token: JwtToken,
      timeout: FiniteDuration
  ): F[Unit] =
    for {
      _ <- redis.setEx(prefixUserId(user.userId), token.value, timeout)
      _ <- redis.setEx(prefixToken(token), user.asJson.toString, timeout)
    } yield ()

  override def delToken(userId: UserId, token: JwtToken): F[Unit] =
    for {
      _ <- redis.del(prefixUserId(userId))
      _ <- redis.del(prefixToken(token))
    } yield ()

  private def prefixToken(token: JwtToken): String = "token:" + token.value
  private def prefixUserId(userId: UserId): String = "userId:" + userId.show
}

object AuthCache {
  def resource[F[_]: Monad](
      redis: RedisCommands[F, String, String]
  ): Resource[F, AuthCache[F]] =
    Resource.pure(AuthCache[F](redis))
}
