package blog.middlewares.auth

import blog.domain.users.User
import blog.storage._
import cats.Monad
import cats.implicits.toFunctorOps
import dev.profunktor.auth.jwt.JwtToken
import io.circe.parser.decode
import pdi.jwt.JwtClaim

object UsersAuth {

  def make[F[_]: Monad](
      cache: AuthCacheDsl[F]
  ): UsersAuthDsl[F, User] = {
    new UsersAuthDsl[F, User] {
      override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[User]] =
        cache
          .getUserAsString(token)
          .map { _.flatMap(u => decode[User](u).toOption) }
    }
  }
}
