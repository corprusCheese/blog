package blog.routes

import blog.domain._
import blog.domain.requests._
import blog.domain.users._
import blog.errors._
import blog.programs.PostProgram
import blog.routes.params._
import blog.utils.ext.refined._
import blog.utils.routes.params._
import cats.MonadThrow
import cats.data.Kleisli
import cats.effect.IO
import cats.syntax.all._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric.NonNegInt
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sLiteralsSyntax
import org.http4s.server.middleware.CORS
import org.http4s.server.{AuthMiddleware, Router}

import scala.util.Try

final case class Posts[F[_]: JsonDecoder: MonadThrow](
    postProgram: PostProgram[F]
) extends Http4sDsl[F] {

  private val prefix: String = "/post"

  object OptionalPageQueryParamMatcher
      extends OptionalQueryParamDecoderMatcherWithDefault[Page]("page", 0)

  implicit val pageQueryParamDecoder: QueryParamDecoder[Page] =
    QueryParamDecoder[Int].emap(x =>
      Try(NonNegInt.unsafeFrom(x)).toEither.leftMap(throwable =>
        ParseFailure(throwable.getMessage, throwable.getMessage)
      )
    )

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

  val getPost = List(GET, POST)

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case (method: Method) -> Root / "all" :? OptionalPageQueryParamMatcher(page) if method == GET || method == POST =>
      postProgram
        .paginatePosts(page)
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound(s"no posts on page $page")
        }

    case GET -> Root / "user" / UserIdVar(userId) =>
      postProgram
        .findByUser(userId)
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no posts for such user")
        }

    case GET -> Root / PostIdVar(postId) =>
      postProgram
        .findById(postId)
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no posts with such id")
        }

    case GET -> Root / "tag" / TagIdVar(tagId) :? OptionalPageQueryParamMatcher(page) =>
      postProgram
        .paginatePostsByTag(page, tagId)
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
    CORS(Router(prefix -> (httpRoutes <+> authMiddleware(httpRoutesAuth))))

}
