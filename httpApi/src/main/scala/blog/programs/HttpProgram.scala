package blog.programs

import blog.impl._
import blog.middlewares.commonAuthMiddleware
import blog.resources.{HttpServer, StorageResources}
import blog.routes.getAll
import cats._
import cats.effect._
import cats.implicits._
import dev.profunktor.redis4cats.effect.MkRedis
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.typelevel.log4cats.Logger

object HttpProgram {

  def run[F[
      _
  ]: Monad: MonadCancelThrow: NonEmptyParallel: Async: Logger: MkRedis]
      : F[Nothing] =
    StorageResources
      .make[F]
      .evalMap(res => {
        val routes = for {
          us <- UserStorage.resource(res.transactor)
          tm <- TokenManager.resource()
          cs <- CommentStorage.resource(res.transactor)
          ps <- PostStorage.resource(res.transactor)
          ts <- TagStorage.resource(res.transactor)
          ac = AuthCommands.make(res.redis, us, tm)
          middleware = commonAuthMiddleware(res.redis)
        } yield
          getAll[F](ac, us, cs, ts, ps, middleware)

        routes.use[Nothing](r => HttpServer[F].newEmber(r.orNotFound).useForever)
      })
      .useForever
}
