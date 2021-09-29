package blog

import blog.domain.users.User
import blog.impl.{AuthCommands, TokenManager, UserStorage}
import blog.resources.{HttpServer, StorageResources}
import blog.routes.Auth
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import org.typelevel.log4cats.{Logger, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import cats._
import cats.data._
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.{JwtNoValidation, JwtToken}
import dev.profunktor.redis4cats.log4cats._
import org.http4s.dsl.io.Forbidden
import org.http4s.{AuthedRoutes, Request}
import org.http4s.server.AuthMiddleware
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.middleware.RequestLogger
import pdi.jwt.JwtClaim

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    StorageResources
      .make[IO]
      .evalMap(resources => {
        val userStorage = UserStorage.resource(resources.transactor)
        val tokenManager = TokenManager.resource()
        def findUser(token: JwtToken)(claim: JwtClaim): IO[Option[User]] =
          resources.redis
            .get(token.value)
            .map { x => x.flatMap(u => decode[User](u).toOption) }

        userStorage.use(us => {
          tokenManager.use(tm => {
            val authCommands = AuthCommands.make(resources.redis, us, tm)
            val middleware =
              JwtAuthMiddleware[IO, User](JwtNoValidation, findUser)
            val routes = Auth(authCommands).routes(middleware)
            HttpServer[IO].newEmber(routes.orNotFound).useForever
          })
        })
      })
      .useForever
  }
}
