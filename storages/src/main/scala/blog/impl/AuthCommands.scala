package blog.impl

import blog.domain._
import blog.domain.users.UserCreate
import blog.storage._
import cats._
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands

import scala.concurrent.duration.FiniteDuration

object AuthCommands {

  def make[F[_]: Monad](
      redis: RedisCommands[F, String, String],
      userStorage: UserStorageDsl[F],
      tokenManager: TokenManagerDsl[F],
      tokenExpiration: FiniteDuration = tokenExpirationDefault
  ): AuthCommandsDsl[F] =
    new AuthCommandsDsl[F] {
      override def newUser(
          userId: UserId,
          username: Username,
          password: HashedPassword
      ): F[Option[JwtToken]] =
        userStorage.findByName(username).flatMap {
          case Some(_) => none[JwtToken].pure[F]
          case None =>
            for {
              _ <- userStorage.create(UserCreate(userId, username, password))
              token <- tokenManager.create
              _ <- redis.setEx(userId.show, token.value, tokenExpiration)
            } yield token.some
        }

      override def login(
          username: Username,
          password: HashedPassword
      ): F[Option[JwtToken]] =
        userStorage.findByName(username).flatMap {
          case Some(user) if password == user.password =>
            for {
              token <- tokenManager.create
              _ <- redis.setEx(user.uuid.show, token.value, tokenExpiration)
            } yield token.some
          case _ => none[JwtToken].pure[F]
        }

      override def logout(
          token: JwtToken,
          userId: UserId
      ): F[Unit] =
        redis.del(userId.show).map(_ => ())
    }
}
