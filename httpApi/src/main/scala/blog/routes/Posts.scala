package blog.routes

import blog.domain._
import blog.domain.requests._
import blog.domain.users._
import blog.errors._
import blog.programs.PostProgram
import blog.utils.ext.refined._
import blog.utils.routes.params._
import cats.MonadThrow
import cats.syntax.all._
import eu.timepit.refined.auto._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

import java.util.UUID

final case class Posts[F[_]: JsonDecoder: MonadThrow](
    postProgram: PostProgram[F]
) extends Http4sDsl[F] {

  private val prefix: String = "/post"

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[PostCreation] { create =>
        postProgram
          .create(
            create.message,
            getVectorFromOptionNev(create.tagIds),
            user.userId
          )
          .flatMap(_ => Created("Post created"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }
    case ar @ POST -> Root / "update" as user =>
      ar.req.decodeR[PostChanging] { update =>
        postProgram
          .update(
            update.postId,
            update.message,
            getVectorFromOptionNev(update.tagIds),
            user.userId
          )
          .flatMap(_ => Ok("Post updated"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }
    case ar @ POST -> Root / "delete" as user =>
      ar.req.decodeR[PostRemoving] { delete =>
        postProgram
          .delete(delete.postId, user.userId)
          .flatMap(_ => Ok("Post deleted"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }
  }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "all" =>
      val page: Page = getPage(req)
      postProgram
        .paginatePosts(page)
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound(s"no posts on page $page")
        }

    case GET -> Root / "user" / userId =>
      postProgram
        .findByUser(UserId(UUID.fromString(userId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no posts for such user")
        }

    case GET -> Root / postId =>
      postProgram
        .findById(PostId(UUID.fromString(postId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no posts with such id")
        }

    case req @ GET -> Root / "tag" / tagId =>
      val page: Page = getPage(req)
      postProgram
        .paginatePostsByTag(page, TagId(UUID.fromString(tagId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound(s"no post with this tag on page $page")
        }
  }

  // routes

  def routesWithoutAuthOnly: HttpRoutes[F] =
    Router(prefix -> httpRoutes)

  def routesWithAuthOnly(
      authMiddleware: AuthMiddleware[F, User]
  ): HttpRoutes[F] =
    Router(prefix -> authMiddleware(httpRoutesAuth))

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    routesWithoutAuthOnly <+> routesWithAuthOnly(authMiddleware)
}
