package blog.storage

import blog.domain.{Password, Username}
import dev.profunktor.auth.jwt.JwtToken

trait AuthCommandsDsl[F[_]] {
  def newUser(username: Username, password: Password): F[JwtToken]
  def login(username: Username, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: Username): F[Unit]
}
