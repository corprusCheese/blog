package blog.impl

import blog.config.types.{JwtAccessTokenKey, TokenExpiration}
import blog.domain
import blog.domain._
import blog.storage._
import cats._
import cats.effect.{Resource, Sync}
import dev.profunktor.auth.jwt._
import org.typelevel.log4cats.Logger
import pdi.jwt._
import cats.implicits._
import io.circe.syntax._
import eu.timepit.refined.auto._

import java.time.Clock
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

case class TokenManager[F[_]: Monad: Sync: Logger](
    tokenExpiration: TokenExpiration,
    jwtAccessTokenKeyConfig: JwtAccessTokenKey
) extends TokenManagerDsl[F] {

  private implicit val clock: F[Clock] = Sync[F].delay(Clock.systemUTC())

  override def create: F[JwtToken] =
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
  ): Resource[F, TokenManagerDsl[F]] =
    Resource.pure(TokenManager[F](tokenExpiration, jwtAccessTokenKeyConfig))
}
