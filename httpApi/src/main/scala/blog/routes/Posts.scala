package blog.routes

import blog.domain._
import blog.domain.posts._
import blog.domain.users._
import blog.domain.requests._

import blog.errors._
import blog.storage.{CommentStorageDsl, PostStorageDsl, TagStorageDsl}
import blog.utils.ext.refined._
import cats.MonadThrow
import cats.data.NonEmptyVector
import cats.syntax.all._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.numeric._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s._

import java.util.UUID

final case class Posts[F[_]: JsonDecoder: MonadThrow](
    postStorage: PostStorageDsl[F],
    commentStorage: CommentStorageDsl[F],
    tagStorage: TagStorageDsl[F]
) extends Http4sDsl[F] {

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[PostCreation] { create =>
        postStorage
          .create(
            CreatePost(
              PostId(UUID.randomUUID()),
              create.message,
              user.uuid,
              create.tagIds.getOrElse(Vector.empty[TagId])
            )
          )
          .flatMap(_ => Created("Post created"))
          .handleErrorWith(_ => throw CreatePostError)
          .recoverWith {
            case e: CustomError => BadRequest(e.msg)
          }
      }
    case ar @ POST -> Root / "update" as user =>
      ar.req.decodeR[PostChanging] { update =>
        postBelongsToUser(user.uuid, update.postId).flatMap {
          case None => throw PostDontBelongToUser
          case _ =>
            postStorage
              .update(
                UpdatePost(
                  update.postId,
                  update.message,
                  update.tagIds.getOrElse(Vector.empty[TagId])
                )
              )
              .flatMap(_ => Ok("Post updated"))
              .handleErrorWith(_ => throw UpdatePostError)
              .recoverWith {
                case e: CustomError => BadRequest(e.msg)
              }
        }
      }
    case ar @ POST -> Root / "delete" as user =>
      ar.req.decodeR[PostRemoving] { delete =>
        postBelongsToUser(user.uuid, delete.postId).flatMap {
          case None => throw PostNotExists
          case _ =>
            commentStorage
              .deleteAllPostComments(delete.postId)
              .handleErrorWith(_ => throw DeletePostCommentError)
              .flatTap(_ =>
                postStorage
                  .delete(
                    DeletePost(delete.postId)
                  )
              )
              .handleErrorWith(_ => throw DeletePostError)
              .flatMap(_ => Ok("Post deleted"))
              .recoverWith {
                case e: CustomError => BadRequest(e.msg)
              }
        }
      }
  }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "all" =>
      val page = getPage(req)
      postStorage
        .fetchForPagination(getPage(req))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound(s"no posts on page ${page}")
        }

    case req @ GET -> Root / "user" / userId =>
      postStorage
        .getAllUserPosts(UserId(UUID.fromString(userId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no posts for such user")
        }

    case req @ GET -> Root / postId =>
      postStorage
        .findById(PostId(UUID.fromString(postId)))
        .flatMap {
          case v if v.nonEmpty => Ok(v)
          case _               => NotFound("no posts with such id")
        }

    case req @ GET -> Root / "tag" / tagId =>
      postStorage
        .fetchPostForPaginationWithTags(
          NonEmptyVector.one(TagId(UUID.fromString(tagId))),
          getPage(req)
        )
        .flatMap {
          case v if v.isEmpty  => NotFound("no post with this tag")
          case v if v.nonEmpty => Ok(v)
        }

    // it is POST query because url is limited
    case req @ POST -> Root / "tags" =>
      req.decodeR[PostFilteredByTags] { filter =>
        postStorage
          .fetchPostForPaginationWithTags(
            filter.tagIds,
            getPage(req)
          )
          .flatMap {
            case v if v.nonEmpty => Ok(v)
            case _               => NotFound("no post with such tags")
          }
      }

  }

  private def postBelongsToUser(
      userId: UserId,
      postId: PostId
  ): F[Option[Post]] =
    postStorage.findById(postId).map {
      case None       => none[Post]
      case Some(post) => if (post.userId == userId) post.some else none[Post]
    }

  private def getPage(req: Request[F]): Page =
    try { NonNegInt.unsafeFrom(req.params("page").toInt) }
    catch { case _: Throwable => 0 }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(
      "/post" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
    )
}
