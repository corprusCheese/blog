package blog.auth


import blog.config.types._
import blog.domain._
import blog.domain.users.{User, UserCreate}
import blog.storage._
import cats._
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import eu.timepit.refined.cats._

trait AuthCommands[F[_]] {
  def newUser(
      userId: UserId,
      username: Username,
      password: HashedPassword
  ): F[Option[JwtToken]]
  def login(username: Username, password: HashedPassword): F[Option[JwtToken]]
  def logout(token: JwtToken, username: UserId): F[Unit]
}

object AuthCommands {

  def make[F[_]: Monad](
      cache: AuthCacheDsl[F],
      userStorage: UserStorageDsl[F],
      tokenManager: TokenManager[F],
      tokenExpiration: TokenExpiration
  ): AuthCommands[F] =
    new AuthCommands[F] {
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
              _ <- cache.setToken(
                User(userId, username, password),
                token,
                tokenExpiration.timeout
              )

            } yield token.some
        }

      override def login(
          username: Username,
          password: HashedPassword
      ): F[Option[JwtToken]] =
        userStorage.findByName(username).flatMap {
          case Some(user) if password == user.password =>
            for {
              redisToken <- cache.getTokenAsString(user.userId)
              token <- redisToken match {
                case None =>
                  for {
                    token <- tokenManager.create
                    _ <- cache.setToken(
                      User(user.userId, username, password),
                      token,
                      tokenExpiration.timeout
                    )
                  } yield token
                case Some(token) => JwtToken(token).pure[F]
              }
            } yield token.some
          case _ => none[JwtToken].pure[F]
        }

      override def logout(
          token: JwtToken,
          userId: UserId
      ): F[Unit] =
        cache.delToken(userId, token)
    }
}
