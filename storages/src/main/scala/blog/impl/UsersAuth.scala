package blog.impl

import blog.domain.users.User
import blog.storage._
import cats.Monad
import cats.implicits.toFunctorOps
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import pdi.jwt.JwtClaim

object UsersAuth {
  // we can implement it for admin user later
  // it should attach to middleware
  def make[F[_]: Monad](
      redis: RedisCommands[F, String, String]
  ): UsersAuthDsl[F, User] = {
    new UsersAuthDsl[F, User] {
      override def findUser(token: JwtToken)(claim: JwtClaim): F[Option[User]] =
        redis
          .get(token.value)
          .map { x => x.flatMap(u => decode[User](u).toOption) }
    }
  }
}
