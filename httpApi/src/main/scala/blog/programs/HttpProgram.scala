package blog.programs

import blog.auth.{AuthCommands, TokenManager}
import blog.config.AppConfig
import blog.impl._
import blog.middlewares.commonAuthMiddleware
import blog.resources.{HttpServer, StorageResources}
import blog.routes.getAll
import cats._
import cats.effect._
import cats.effect.kernel.Spawn
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}

object HttpProgram {

  def run[F[_]: MonadCancelThrow: NonEmptyParallel: Async](
      appConfig: AppConfig
  ): F[Nothing] = {
    implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

    StorageResources
      .make[F](appConfig.postgresConfig, appConfig.redisConfig)
      .flatMap(
        routesResource(_, appConfig).flatMap(r =>
          HttpServer[F].newEmber(r.orNotFound, appConfig.httpServerConfig)
        )
      )
      .use[Nothing](_ => Async[F].never)
  }

  private def routesResource[F[_]: MonadCancelThrow: Async: Logger](
      res: StorageResources[F],
      appConfig: AppConfig
  ): Resource[F, HttpRoutes[F]] =
    for {
      // storages works without config
      us <- UserStorage.resource(res.transactor)
      cs <- CommentStorage.resource(res.transactor)
      ts <- TagStorage.resource(res.transactor)
      ps <- PostStorage.resource(
        res.transactor,
        appConfig.paginationOptions.perPage
      )
      // programs works with config
      tm <- TokenManager.resource(
        appConfig.tokenExpiration,
        appConfig.jwtAccessTokenKey
      )
      ac <- AuthCache.resource(res.redis)
      authCommands = AuthCommands.make(ac, us, tm, appConfig.tokenExpiration)
      middleware = commonAuthMiddleware(ac, appConfig.jwtSecretKey)
    } yield getAll[F](us, cs, ts, ps, middleware, authCommands)
}
