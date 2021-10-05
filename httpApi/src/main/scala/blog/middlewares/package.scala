package blog

import blog.config.JwtSecretKey
import blog.domain.users.User
import blog.middlewares.auth.UsersAuth
import blog.storage.AuthCacheDsl
import cats.{Monad, MonadThrow}
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtAuth
import eu.timepit.refined.auto._
import org.http4s.server.AuthMiddleware
import pdi.jwt.JwtAlgorithm

package object middlewares {
  def commonAuthMiddleware[F[_]: Monad: MonadThrow](
      authCacheDsl: AuthCacheDsl[F],
      jwtAuth: JwtSecretKey
  ): AuthMiddleware[F, User] =
    JwtAuthMiddleware[F, User](
      JwtAuth.hmac(jwtAuth.value, JwtAlgorithm.HS256),
      UsersAuth.make(authCacheDsl).findUser
    )
}
