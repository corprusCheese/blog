package blog.routes

import blog.auth.AuthCommands
import blog.domain._
import blog.domain.users.User
import blog.errors._
import blog.domain.requests._
import blog.utils.PassHasher
import blog.utils.ext.refined._
import cats.MonadThrow
import cats.syntax.all._
import dev.profunktor.auth.AuthHeaders
import org.http4s._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

import java.util.UUID

final case class Auth[F[_]: JsonDecoder: MonadThrow](
    authCommands: AuthCommands[F]
) extends Http4sDsl[F] {

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "login" =>
      req.decodeR[LoginUser] { user =>
        authCommands
          .login(user.username, PassHasher.hash(user.password))
          .flatMap {
            case None    => throw LoginError
            case Some(x) => Ok(x.value)
          }
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
            case _ => Forbidden()
          }
      }

    case req @ POST -> Root / "register" =>
      req.decodeR[LoginUser] { user =>
        authCommands
          .newUser(
            UserId(UUID.randomUUID()),
            user.username,
            PassHasher.hash(user.password)
          )
          .flatMap {
            case None =>
              throw RegisterError
            case Some(x) => Ok(x.value)
          }
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
            case _ => Forbidden()
          }
      }
  }

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "logout" as user =>
      AuthHeaders
        .getBearerToken(ar.req)
        .traverse_(authCommands.logout(_, user.uuid)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(
      "/auth" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
    )
}
