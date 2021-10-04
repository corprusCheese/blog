package blog.routes

import blog.domain._
import blog.domain.requests._
import blog.domain.tags._
import blog.domain.users._
import blog.errors._
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

final case class Tags[F[_]: JsonDecoder: MonadThrow](
    tagStorage: TagStorageDsl[F],
    postStorage: PostStorageDsl[F]
) extends Http4sDsl[F] {

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as _ =>
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
        canUserUpdateAllRequestedPosts(user.uuid, update.postIds)
          .flatMap {
            case false => throw UpdateTagWithNotYoursPosts
            case true => tagStorage
              .update(
                TagUpdate(
                  update.tagId,
                  update.name,
                  update.postIds.get.toVector
                )
              )
              .flatMap(_ => Ok("Tag updated"))
              .handleErrorWith(_ => throw UpdateTagError)
              .recoverWith {
                case e: CustomError => BadRequest(e.msg)
              }
          }

      }
  }

  private def canUserUpdateAllRequestedPosts(
      userId: UserId,
      postIds: Option[NonEmptyVector[PostId]]
  ): F[Boolean] =
    postIds match {
      case None => false.pure[F]
      case Some(requestedPostIds) =>
        postStorage
          .getAllUserPosts(userId)
          .map(_.map(_.postId).intersect(requestedPostIds.toVector))
          .map {
            case v if v.isEmpty => false
            case v              => NonEmptyVector.fromVectorUnsafe(v) == requestedPostIds
          }
    }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "all" =>
      tagStorage.fetchAll
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tags at all")
        }

    case GET -> Root / "post" / postId =>
      tagStorage
        .getAllPostTags(PostId(UUID.fromString(postId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tags of such post")
        }

    case GET -> Root / tagId =>
      tagStorage
        .findById(TagId(UUID.fromString(tagId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tags with such id")
        }

    case GET -> Root / tagName =>
      tagStorage
        .findByName(TagName(NonEmptyString.unsafeFrom(tagName)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no tag with such name")
        }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(
      "/tag" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
    )
}
