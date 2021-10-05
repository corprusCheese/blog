package blog.storage

import blog.domain.UserId
import blog.domain.users.User
import dev.profunktor.auth.jwt.JwtToken

import scala.concurrent.duration.FiniteDuration

trait AuthCacheDsl[F[_]] {
  def getTokenAsString(userId: UserId): F[Option[String]]
  def getUserAsString(token: JwtToken): F[Option[String]]
  def setToken(user: User, token: JwtToken, timeout: FiniteDuration): F[Unit]
  def delToken(userId: UserId, token: JwtToken): F[Unit]
}
