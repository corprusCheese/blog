package blog.routes

import blog.domain.comments._
import blog.domain._
import blog.domain.users.User
import blog.domain.requests._
import blog.errors._
import blog.programs.CommentProgram
import blog.storage.{CommentStorageDsl, PostStorageDsl}
import cats.MonadThrow
import cats.implicits._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import blog.utils.ext.refined._
import io.circe.Encoder.AsArray.importedAsArrayEncoder

import java.util.UUID

final case class Comments[F[_]: JsonDecoder: MonadThrow](
    commentProgram: CommentProgram[F]
) extends Http4sDsl[F] {

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[CommentCreation] { create =>
        commentProgram
          .create(create.message, user.userId, create.postId, create.commentId)
          .flatMap(_ => Created("Comment created"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }

    case ar @ POST -> Root / "update" as user =>
      ar.req.decodeR[CommentChanging] { update =>
        commentProgram
          .update(update.commentId, update.message, user.userId)
          .flatMap(_ => Ok("Comment updated"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }

    case ar @ POST -> Root / "delete" as user =>
      ar.req.decodeR[CommentRemoving] { delete =>
        commentProgram.delete(delete.commentId, user.userId)
          .flatMap(_ => Ok("Comment deleted"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }
  }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "all" =>
      commentProgram.getAll.flatMap {
        case v if v.nonEmpty => Ok(v)
        case _               => NotFound("no comments at all")
      }

    case GET -> Root / commentId =>
      commentProgram
        .findById(CommentId(UUID.fromString(commentId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no comments with such id")
        }

    case GET -> Root / "post" / postId =>
      commentProgram
        .getAllPostComments(PostId(UUID.fromString(postId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no comments of such post id")
        }

    case GET -> Root / "user" / userId =>
      commentProgram
        .getActiveUserComments(UserId(UUID.fromString(userId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no comments of such user id")
        }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(
      "/comment" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
    )
}
