package blog.programs

import blog.auth.AuthCommands
import blog.domain._
import blog.errors.{LoginError, RegisterError}
import cats.MonadThrow
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import org.http4s.circe.JsonDecoder

import java.util.UUID

trait AuthProgram[F[_]] {
  def login(username: Username, password: HashedPassword): F[JwtToken]
  def register(username: Username, password: HashedPassword): F[JwtToken]
  def logout(jwtToken: JwtToken, userId: UserId): F[Unit]
}

object AuthProgram {
  def make[F[_]: JsonDecoder: MonadThrow](
      authCommands: AuthCommands[F]
  ): AuthProgram[F] =
    new AuthProgram[F] {
      override def login(
          username: Username,
          password: HashedPassword
      ): F[JwtToken] =
        authCommands
          .login(username, password)
          .map {
            case None    => throw LoginError
            case Some(x) => x
          }

      override def register(
          username: Username,
          password: HashedPassword
      ): F[JwtToken] =
        authCommands
          .newUser(
            UserId(UUID.randomUUID()),
            username,
            password
          )
          .map {
            case None =>
              throw RegisterError
            case Some(x) => x
          }

      override def logout(jwtToken: JwtToken, userId: UserId): F[Unit] =
        authCommands.logout(jwtToken, userId)
    }
}
