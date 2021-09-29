package blog.storage

import blog.domain.{HashedPassword, Password, UserId, Username}
import dev.profunktor.auth.jwt.JwtToken

trait AuthCommandsDsl[F[_]] {
  def newUser(userId: UserId, username: Username, password: HashedPassword): F[Option[JwtToken]]
  def login(username: Username, password: HashedPassword): F[Option[JwtToken]]
  def logout(token: JwtToken, username: UserId): F[Unit]
}
