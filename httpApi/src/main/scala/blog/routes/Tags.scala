package blog.routes

import blog.domain._
import blog.domain.requests._
import blog.domain.tags._
import blog.domain.users._
import blog.errors._
import blog.programs.TagProgram
import blog.storage.{PostStorageDsl, TagStorageDsl}
import blog.utils.ext.refined._
import cats.MonadThrow
import cats.data.NonEmptyVector
import cats.syntax.all._
import eu.timepit.refined.types.all.NonEmptyString
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

import java.util.UUID
import blog.utils.routes.params._


final case class Tags[F[_]: JsonDecoder: MonadThrow](
    tagProgram: TagProgram[F]
) extends Http4sDsl[F] {

  private val prefix: String = "/tag"

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as _ =>
      ar.req.decodeR[TagCreation] { create =>
        tagProgram.create(create.name, getVectorFromOptionNev(create.postIds))
          .handleErrorWith(_ => throw CreateTagError)
          .flatMap(_ => Created("Tag created"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }

    case ar @ POST -> Root / "update" as user =>
      ar.req.decodeR[TagChanging] { update =>
        tagProgram.update(update.tagId, update.name, getVectorFromOptionNev(update.postIds), user.userId)
          .flatMap(_ => Ok("Tag updated"))
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }
  }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "all" =>
      tagProgram.getAll
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tags at all")
        }

    case GET -> Root / "post" / postId =>
      tagProgram.getPostTags(PostId(UUID.fromString(postId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tags of such post")
        }

    case GET -> Root / "id" / tagId =>
      tagProgram.findById(TagId(UUID.fromString(tagId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tags with such id")
        }

    case GET -> Root / "name" / tagName =>
      tagProgram.findByName(TagName(NonEmptyString.unsafeFrom(tagName)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tag with such name")
        }
  }

  // routes

  def routesWithoutAuthOnly: HttpRoutes[F] =
    Router(prefix -> httpRoutes)

  def routesWithAuthOnly(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(prefix -> authMiddleware(httpRoutesAuth))

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    routesWithoutAuthOnly <+> routesWithAuthOnly(authMiddleware)
}
