package blog.config

import blog.domain.PerPage
import cats.MonadThrow
import cats.effect.Resource
import cats.implicits._
import com.comcast.ip4s.{Host, Port}
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.types.string.NonEmptyString
import pureconfig._
import pureconfig.generic.auto._

import scala.concurrent.duration.FiniteDuration

case class HttpServerConfig(
    host: Host,
    port: Port
)

case class TokenExpiration(timeout: FiniteDuration)

case class RedisConfig(uri: NonEmptyString)

// for creating token
case class JwtAccessTokenKey(value: NonEmptyString)

// for user middleware
case class JwtSecretKey(value: NonEmptyString)

case class PostgresConfig(
    host: Host,
    port: Port,
    db: NonEmptyString,
    user: NonEmptyString,
    password: NonEmptyString
) {
  def postgresUri: String = s"jdbc:postgresql://${host}:${port}/${db}"
  def commonDriver: String = "org.postgresql.Driver"
}

case class PaginationOptions(
    perPage: PerPage
)

case class AppConfig(
    tokenExpiration: TokenExpiration,
    httpServerConfig: HttpServerConfig,
    redisConfig: RedisConfig,
    postgresConfig: PostgresConfig,
    jwtAccessTokenKey: JwtAccessTokenKey,
    jwtSecretKey: JwtSecretKey,
    paginationOptions: PaginationOptions
)

object config {
  def make[F[_]: MonadThrow]: Resource[F, AppConfig] =
    Resource.eval(
      MonadThrow[F].fromEither(
        ConfigSource.default
          .load[AppConfig]
          .leftMap(error => throw new Exception("config error: " + error.head.description))
      )
    )

  implicit val nesReader: ConfigReader[NonEmptyString] =
    ConfigReader[String].map(s => {
      if (s.trim().isEmpty) throw new Exception("empty NonEmptyString")
      else NonEmptyString.unsafeFrom(s)
    })

  implicit val hostReader: ConfigReader[Host] = ConfigReader[String].map(s => {
    Host.fromString(s).getOrElse(throw new Exception("empty Host"))
  })

  implicit val portReader: ConfigReader[Port] = ConfigReader[Int].map(i => {
    Port.fromInt(i).getOrElse(throw new Exception("Cannot convert to Port"))
  })

  implicit val perPageReader: ConfigReader[PerPage] =
    ConfigReader[Int].map(i => {
      if (i < 0) throw new Exception("needs non negative PerPage")
      else NonNegInt.unsafeFrom(i)
    })
}
