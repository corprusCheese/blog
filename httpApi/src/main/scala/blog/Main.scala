package blog

import blog.domain.users.User
import blog.impl.{AuthCommands, TokenManager, UserStorage, UsersAuth}
import blog.resources.{HttpServer, StorageResources}
import blog.routes.Auth
import cats.effect.{ExitCode, IO, IOApp}
import dev.profunktor.auth.JwtAuthMiddleware
import dev.profunktor.auth.jwt.JwtAuth
import dev.profunktor.redis4cats.log4cats._
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import pdi.jwt._

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    StorageResources
      .make[IO]
      .evalMap(resources => {
        val userStorage = UserStorage.resource(resources.transactor)
        val tokenManager = TokenManager.resource()
        userStorage.use(us => {
          tokenManager.use(tm => {
            val authCommands = AuthCommands.make(resources.redis, us, tm)
            val middleware =
              JwtAuthMiddleware[IO, User](
                JwtAuth.hmac("secret", JwtAlgorithm.HS256),
                UsersAuth.make(resources.redis).findUser
              )
            val routes = Auth(authCommands).routes(middleware)
            HttpServer[IO].newEmber(routes.orNotFound).useForever
          })
        })
      })
      .useForever
  }
}
