package blog.resources

import blog.config.types.{AppConfig, PostgresConfig, RedisConfig}
import cats._
import cats.effect._
import cats.implicits.catsSyntaxTuple3Parallel
import dev.profunktor.redis4cats.effect.MkRedis
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import doobie.Transactor
import doobie.util.transactor.Transactor.Aux
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._

sealed abstract class StorageResources[F[_]] {
  val transactor: Aux[F, Unit]
  val redis: RedisCommands[F, String, String]
  val client: Client[F]
}

object StorageResources {
  def make[F[_]: Logger: Monad: Async: MkRedis: HttpClient: NonEmptyParallel](
      postgresConfig: PostgresConfig,
      redisConfig: RedisConfig
  ): Resource[F, StorageResources[F]] = {
    def postgresResource: Resource[F, Aux[F, Unit]] =
      Resource
        .pure[F, Aux[F, Unit]](
          Transactor.fromDriverManager[F](
            postgresConfig.commonDriver,
            postgresConfig.postgresUri,
            postgresConfig.user,
            postgresConfig.password
          )
        )

    def redisResource: Resource[F, RedisCommands[F, String, String]] =
      Redis[F].utf8(redisConfig.uri)

    def clientResource: Resource[F, Client[F]] = HttpClient[F].newEmber

    (
      postgresResource,
      redisResource,
      clientResource
    ).parMapN((p, r, c) =>
      new StorageResources[F] {
        override val transactor: Aux[F, Unit] = p
        override val redis: RedisCommands[F, String, String] = r
        override val client: Client[F] = c
      }
    )

  }
}
