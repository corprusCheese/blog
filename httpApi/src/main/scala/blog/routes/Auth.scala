package blog.routes

import blog.domain.requests._
import blog.domain.users.User
import blog.errors._
import blog.programs.AuthProgram
import blog.utils.PassHasher
import blog.utils.ext.refined._
import cats.MonadThrow
import cats.syntax.all._
import dev.profunktor.auth.AuthHeaders
import org.http4s._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.http4s.server.{AuthMiddleware, Router}

final case class Auth[F[_]: JsonDecoder: MonadThrow](
    authProgram: AuthProgram[F]
) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginUser] { user =>
        authProgram
          .login(user.username, PassHasher.hash(user.password))
          .flatMap(jwt => Ok(jwt.value))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
            case _              => Forbidden()
          }
      }

    case req @ POST -> Root / "register" =>
      req.decodeR[LoginUser] { user =>
        authProgram
          .register(user.username, PassHasher.hash(user.password))
          .flatMap(jwt => Ok(jwt.value))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
            case _              => Forbidden()
          }
      }
  }

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(jwt => authProgram.logout(jwt, user.userId)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    CORS(
      Router(
        "/auth" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
      )
    )
}
