package blog.impl

import blog.domain._
import blog.storage._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands

import scala.concurrent.duration.FiniteDuration

object AuthCommands {

  def make[F[_]](
      tokenExpiration: FiniteDuration = tokenExpirationDefault,
      redis: RedisCommands[F, String, String],
      userStorage: UserStorageDsl[F],
      tokenManager: TokenManagerDsl[F]
  ): AuthCommandsDsl[F] =
    new AuthCommandsDsl[F] {
      override def newUser(
          username: Username,
          password: Password
      ): F[JwtToken] = ???

      override def login(
          username: Username,
          password: Password
      ): F[JwtToken] = ???

      override def logout(token: JwtToken, username: Username): F[Unit] = ???
    }
}
