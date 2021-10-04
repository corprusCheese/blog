package blog.config

import blog.config.types._
import cats.effect.Resource
import com.comcast.ip4s._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import scala.concurrent.duration.DurationInt

object config {
  def make[F[_]]: Resource[F, AppConfig] =
    Resource.pure[F, AppConfig](
      AppConfig(
        TokenExpiration(60.minutes),
        Http4sServerConfig(
          host"localhost",
          port"8080"
        ),
        RedisConfig("redis://0.0.0.0:6379"),
        PostgresConfig(
          host"0.0.0.0",
          port"5432",
          "blog",
          "admin",
          "password"
        ),
        JwtAccessTokenKey("secret"),
        JwtSecretKey("secret"),
        PaginationOptions(perPage = 10)
      )
    )

  def makeForTest[F[_]]: Resource[F, AppConfig] =
    Resource.pure[F, AppConfig](
      AppConfig(
        TokenExpiration(30.seconds),
        Http4sServerConfig(
          host"localhost",
          port"8080"
        ),
        RedisConfig("redis://0.0.0.0:6380"),
        PostgresConfig(
          host"0.0.0.0",
          port"5433",
          "blog",
          "admin",
          "password"
        ),
        JwtAccessTokenKey("secret"),
        JwtSecretKey("secret"),
        PaginationOptions(perPage = 1000)
      )
    )
}
