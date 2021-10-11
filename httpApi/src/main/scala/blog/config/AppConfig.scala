package blog.config

import blog.config.types._
import blog.domain._
import cats.MonadThrow
import cats.effect.Resource
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.pureconfig._
import pureconfig._
import pureconfig.generic.auto._

case class AppConfig(
    tokenExpiration: TokenExpiration,
    httpServerConfig: HttpServerConfig,
    redisConfig: RedisConfig,
    postgresConfig: PostgresConfig,
    jwtAccessTokenKey: JwtAccessTokenKey,
    jwtSecretKey: JwtConfigSecretKey,
    paginationOptions: PaginationOptions
)

object AppConfig {
  def make[F[_]: MonadThrow]: Resource[F, AppConfig] =
    Resource.pure(ConfigSource.default.loadOrThrow[AppConfig])
}
