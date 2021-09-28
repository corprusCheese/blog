package blog.impl

import blog.domain.tokenExpirationDefault
import blog.storage.TokenManagerDsl
import cats.effect.Resource
import dev.profunktor.auth.jwt._
import org.typelevel.log4cats.Logger

import scala.concurrent.duration.FiniteDuration

case class TokenManager[F[_]: Logger](tokenExpiration: FiniteDuration)
    extends TokenManagerDsl[F] {
  override def create: F[JwtToken] = ???
}

object TokenManager {
  def resource[F[_]: Logger](
      tokenExpiration: FiniteDuration = tokenExpirationDefault
  ): Resource[F, TokenManagerDsl[F]] =
    Resource.pure(TokenManager[F](tokenExpiration))
}
