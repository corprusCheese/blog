package blog.storage

import dev.profunktor.auth.jwt.JwtToken

trait TokenManagerDsl [F[_]]{
  def create: F[JwtToken]
}
