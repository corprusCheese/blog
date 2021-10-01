package blog.routes

import blog.domain.comments._
import blog.domain._
import blog.domain.users.User
import blog.errors._
import blog.storage.{CommentStorageDsl, PostStorageDsl}
import cats.MonadThrow
import cats.implicits._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import blog.utils.ext.refined._

import java.util.UUID

final case class Comments[F[_]: JsonDecoder: MonadThrow](
    commentStorage: CommentStorageDsl[F],
    postStorage: PostStorageDsl[F]
) extends Http4sDsl[F] {

  private val httpRoutesAuth: AuthedRoutes[User, F] = AuthedRoutes.of {
    case ar @ POST -> Root / "create" as user =>
      ar.req.decodeR[CommentCreation] { create =>
        getPath(create.postId, create.commentId).flatMap(p =>
          commentStorage
            .create(
              CreateComment(
                CommentId(UUID.randomUUID()),
                create.message,
                user.uuid,
                CommentMaterializedPath(p)
              )
            )
            .flatMap(_ => Created("Comment created"))
            .handleErrorWith(_ => throw CreateCommentError)
            .recoverWith {
              case e: CustomError => BadRequest(e.msg)
            }
        )

      }
    case ar @ POST -> Root / "update" as user =>
      ar.req.decodeR[CommentChanging] { update =>
        commentBelongsToUser(user.uuid, update.commentId).flatMap {
          case None => throw CommentDontBelongToUser
          case Some(comment) =>
            commentStorage
              .update(
                UpdateComment(
                  comment.commentId,
                  comment.message
                )
              )
              .flatMap(_ => Ok("Comment updated"))
              .handleErrorWith(_ => throw UpdateCommentError)
              .recoverWith {
                case e: CustomError => BadRequest(e.msg)
              }
        }
      }

    case ar @ POST -> Root / "delete" as user =>
      ar.req.decodeR[CommentRemoving] { delete =>
        commentBelongsToUser(user.uuid, delete.commentId).flatMap {
          case None => throw CommentNotExists
          case Some(comment) =>
            commentStorage
              .delete(DeleteComment(comment.commentId))
              .flatMap(_ => Ok("Comment deleted"))
              .handleErrorWith(_ => throw DeleteCommentError)
              .recoverWith {
                case e: CustomError => BadRequest(e.msg)
              }
        }
      }
  }

  private def getPath(
      postId: PostId,
      commentIdOpt: Option[CommentId]
  ): F[LTree] =
    commentIdOpt match {
      case None => postId.toString.pure[F]
      case Some(commentId) =>
        commentStorage.findById(commentId).map {
          case None          => throw NoSuchCommentId
          case Some(comment) => comment.path + "." + commentId
        }
    }

  private def commentBelongsToUser(
      userId: UserId,
      commentId: CommentId
  ): F[Option[Comment]] =
    commentStorage.findById(commentId).map {
      case None => none[Comment]
      case Some(comment) =>
        if (comment.userId == userId) comment.some else none[Comment]
    }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ GET -> Root / "all" =>
      commentStorage.fetchAll.flatMap{
        case v if v.isEmpty => NotFound("no comments at all")
        case v if v.nonEmpty => Ok(v)
      }
    case req @ GET -> Root / commentId =>
      commentStorage
        .findById(CommentId(UUID.fromString(commentId)))
        .flatMap{
          case v if v.isEmpty => NotFound("no comments with such id")
          case v if v.nonEmpty => Ok(v)
        }
    case req @ GET -> Root / "post" / postId =>
      commentStorage
        .getAllPostComments(PostId(UUID.fromString(postId)))
        .flatMap{
          case v if v.isEmpty => NotFound("no comments of such post id")
          case v if v.nonEmpty => Ok(v)
        }
    case req @ GET -> Root / "user" / userId =>
      commentStorage
        .getAllUserComments(UserId(UUID.fromString(userId)))
        .flatMap{
          case v if v.isEmpty => NotFound("no comments of such user id")
          case v if v.nonEmpty => Ok(v)
        }
  }

  def routes(authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] =
    Router(
      "/comment" -> (httpRoutes <+> authMiddleware(httpRoutesAuth))
    )
}
