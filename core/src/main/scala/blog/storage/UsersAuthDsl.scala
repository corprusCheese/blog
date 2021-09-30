package blog.storage

import dev.profunktor.auth.jwt.JwtToken
import pdi.jwt.JwtClaim

trait UsersAuthDsl[F[_], A] {
  def findUser(token: JwtToken)(claim: JwtClaim): F[Option[A]]
}
