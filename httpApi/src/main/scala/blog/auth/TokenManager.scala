package blog.auth

import blog.config
import blog.config.types._
import cats._
import cats.effect.{Resource, Sync}
import cats.implicits._
import dev.profunktor.auth.jwt.{JwtSecretKey, JwtToken, jwtEncode}
import eu.timepit.refined.auto._
import io.circe.syntax._
import org.typelevel.log4cats.Logger
import pdi.jwt._

import java.time.Clock
import java.util.UUID

case class TokenManager[F[_]: Monad: Sync: Logger](
    tokenExpiration: TokenExpiration,
    jwtAccessTokenKeyConfig: JwtAccessTokenKey
) {

  private implicit val clock: F[Clock] = Sync[F].delay(Clock.systemUTC())

  def create: F[JwtToken] =
    for {
      uuid <- UUID.randomUUID().pure[F]
      clock <- Sync[F].delay(Clock.systemUTC())
      claim <- Sync[F].delay(
        JwtClaim(uuid.asJson.noSpaces)
          .issuedNow(clock)
          .expiresIn(tokenExpiration.timeout.toSeconds)(clock)
      )
      secretKey = JwtSecretKey(jwtAccessTokenKeyConfig.value)
      token <- jwtEncode[F](claim, secretKey, JwtAlgorithm.HS256)
    } yield token
}

object TokenManager {
  def resource[F[_]: Logger: Monad: Sync](
      tokenExpiration: TokenExpiration,
      jwtAccessTokenKeyConfig: JwtAccessTokenKey
  ): Resource[F, TokenManager[F]] =
    Resource.pure(TokenManager[F](tokenExpiration, jwtAccessTokenKeyConfig))
}
