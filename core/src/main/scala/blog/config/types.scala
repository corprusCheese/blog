package blog.config

import blog.domain._
import com.comcast.ip4s.{Host, Port}
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object types {

  case class Http4sServerConfig(
      host: Host,
      port: Port
  )

  @newtype
  case class TokenExpiration(timeout: FiniteDuration)

  @newtype
  case class RedisConfig(uri: NonEmptyString)

  // for creating token
  @newtype
  case class JwtAccessTokenKey(value: NonEmptyString)

  // for user middleware
  @newtype
  case class JwtSecretKey(secret: NonEmptyString)

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

  case class AppConfig(
      tokenExpiration: TokenExpiration,
      http4sServerConfig: Http4sServerConfig,
      redisConfig: RedisConfig,
      postgresConfig: PostgresConfig,
      jwtAccessTokenKey: JwtAccessTokenKey,
      jwtSecretKey: JwtSecretKey,
      paginationOptions: PaginationOptions
  )

  case class PaginationOptions(
      perPage: PerPage
  )
}
