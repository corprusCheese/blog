package blog.routes

import blog.domain._
import blog.domain.tags.{TagCreate, TagUpdate}
import blog.domain.users._
import blog.errors._
import blog.domain.requests._
import blog.storage.{PostStorageDsl, TagStorageDsl}
import blog.utils.ext.refined._
import cats.MonadThrow
import cats.syntax.all._
import eu.timepit.refined.types.all.NonEmptyString
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}

import java.util.UUID
import scala.util.control.NoStackTrace

final case class Tags[F[_]: JsonDecoder: MonadThrow](
    tagStorage: TagStorageDsl[F],
    postStorage: PostStorageDsl[F]
) extends Http4sDsl[F] {

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[TagCreation] { create =>
        tagStorage
          .create(
            TagCreate(
              TagId(UUID.randomUUID()),
              create.name,
              if (create.postId.isEmpty) Vector.empty[PostId]
              else Vector[PostId](create.postId.get)
            )
          )
          .flatMap(_ => Created("Tag created"))
          .handleErrorWith(_ => throw CreateTagError)
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }

    case ar @ POST -> Root / "update" as user =>
      ar.req.decodeR[TagChanging] { update =>
        getUserPostIds(user.uuid, update.postIds)
          .map(x => getVector(x, update.postIds, UpdateTagWithNotYoursPosts))
          .flatMap(vec => {
            println(vec)
            tagStorage
              .update(
                TagUpdate(
                  update.tagId,
                  update.name,
                  vec
                )
              )
              .flatMap(_ => Ok("Tag updated"))
              .handleErrorWith(_ => throw UpdateTagError)
              .recoverWith {
                case e: CustomError => BadRequest(e.msg)
              }
          })
      }
  }

  private def getUserPostIds(
      userId: UserId,
      postIds: Option[Vector[PostId]]
  ): F[Option[Vector[PostId]]] =
    postIds match {
      case None => none[Vector[PostId]].pure[F]
      case Some(vector) =>
        postStorage
          .getAllUserPosts(userId)
          .map(_.filter(p => vector.contains(p.postId)))
          .map(v =>
            if (v.isEmpty) none[Vector[PostId]] else v.map(_.postId).some
          )
    }

  private def getVector(
      vec: Option[Vector[PostId]],
      postIds: Option[Vector[PostId]],
      noStackTrace: NoStackTrace
  ): Vector[PostId] =
    vec match {
      case Some(vector) if postIds.getOrElse(Vector.empty[PostId]) == vector =>
        vector
      case None if postIds.isEmpty => Vector.empty[PostId]
      case _                       => throw noStackTrace
    }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "all" =>
      tagStorage.fetchAll
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _  => NotFound("no tags at all")
        }

    case req @ GET -> Root / "post" / postId =>
      tagStorage
        .getAllPostTags(PostId(UUID.fromString(postId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _  => NotFound("no tags of such post")
        }

    case req @ GET -> Root / tagId =>
      tagStorage
        .findById(TagId(UUID.fromString(tagId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _  => NotFound("no tags with such id")
        }

    case req @ GET -> Root / tagName =>
      tagStorage
        .findByName(TagName(NonEmptyString.unsafeFrom(tagName)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _  => NotFound("no tag with such name")
        }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(
      "/tag" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
    )
}
