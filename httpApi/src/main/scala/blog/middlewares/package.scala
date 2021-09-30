package blog

import blog.domain.users.User
import blog.impl.UsersAuth
import cats.{Monad, MonadThrow}
import cats.effect.IO
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.RedisCommands
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm

package object middlewares {
  def commonAuthMiddleware[F[_]: Monad: MonadThrow](
      redis: RedisCommands[F, String, String]
  ): AuthMiddleware[F, User] =
    JwtAuthMiddleware[F, User](
      JwtAuth.hmac("secret", JwtAlgorithm.HS256),
      UsersAuth.make(redis).findUser
    )
}
