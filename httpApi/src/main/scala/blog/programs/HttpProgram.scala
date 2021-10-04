package blog.programs

import blog.config.types.AppConfig
import blog.impl._
import blog.middlewares.commonAuthMiddleware
import blog.resources.{HttpServer, StorageResources}
import blog.routes.getAll
import cats._
import cats.effect._
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import org.http4s.implicits._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._

object HttpProgram {

  def run[F[
      _
  ]: Monad: MonadCancelThrow: NonEmptyParallel: Async](
      appConfig: AppConfig
  ): F[Nothing] = {
    implicit val logger: SelfAwareStructuredLogger[F] = Slf4jLogger.getLogger[F]

    StorageResources
      .make[F](appConfig.postgresConfig, appConfig.redisConfig)
      .evalMap(res => {
        val routes = for {
          us <- UserStorage.resource(res.transactor)
          cs <- CommentStorage.resource(res.transactor)
          ts <- TagStorage.resource(res.transactor)
          ps <- PostStorage.resource(res.transactor, appConfig.paginationOptions)
          tm <- TokenManager.resource(appConfig.tokenExpiration, appConfig.jwtAccessTokenKey)
          ac = AuthCommands.make(res.redis, us, tm, appConfig.tokenExpiration)
          middleware = commonAuthMiddleware(res.redis)
        } yield getAll[F](ac, us, cs, ts, ps, middleware)

        routes
          .use[Nothing](r => HttpServer[F].newEmber(r.orNotFound, appConfig.http4sServerConfig).useForever)
      })
      .useForever
  }
}
