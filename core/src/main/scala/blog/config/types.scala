package blog.config

import blog.domain.PerPage
import eu.timepit.refined.types.all.NonEmptyString
import io.estatico.newtype.macros.newtype

import scala.concurrent.duration.FiniteDuration

object types {
  case class HttpServerConfig(
      host: NonEmptyString,
      port: NonEmptyString
  )

  // todo: newtype
  //@newtype
  case class TokenExpiration(timeout: FiniteDuration)

  //@newtype
  case class RedisConfig(uri: NonEmptyString)

  //@newtype
  case class JwtAccessTokenKey(value: NonEmptyString)

  //@newtype
  case class JwtConfigSecretKey(value: NonEmptyString)

  //@newtype
  case class PaginationOptions(perPage: PerPage)

  case class PostgresConfig(
      host: NonEmptyString,
      port: NonEmptyString,
      db: NonEmptyString,
      user: NonEmptyString,
      password: NonEmptyString
  ) {
    def postgresUri: String = s"jdbc:postgresql://${host}:${port}/${db}"
    def commonDriver: String = "org.postgresql.Driver"
  }
}
