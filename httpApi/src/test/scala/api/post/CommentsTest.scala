package api.post

import api.suites.TestAuth
import blog.config.types.JwtConfigSecretKey
import blog.domain.CommentId
import blog.domain.comments.CreateComment
import blog.domain.posts.CreatePost
import blog.domain.requests.{CommentChanging, CommentCreation, CommentRemoving}
import blog.middlewares.commonAuthMiddleware
import blog.programs.CommentProgram
import blog.programs.comment.PathHandlerProgram
import blog.routes.Comments
import blog.storage._
import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import gen.generators._
import org.http4s.HttpRoutes
import org.http4s.Status._
import org.http4s.syntax.literals._

import java.util.UUID

object CommentsTest extends TestAuth {

  private def routesForTesting(
      cs: CommentStorageDsl[IO],
      ps: PostStorageDsl[IO],
      ac: AuthCacheDsl[IO]
  ): HttpRoutes[IO] =
    Comments[IO](CommentProgram.make(cs, ps)).routesWithAuthOnly(
      commonAuthMiddleware(
        ac,
        JwtConfigSecretKey(
          NonEmptyString.unsafeFrom("secret")
        )
      )
    )

  test("create route") {
    val gen = for {
      u <- userGen
      p <- postGen
      c1 <- commentGen
      c2 <- commentGen
    } yield (u, p, c1, c2)

    forall(gen) {
      case (user, post, comment1, comment2) =>
        resourceStorages.use {
          case (_, ps, cs, ts, authCache, ac) =>
            val pathHandlerProgram = PathHandlerProgram.make(cs)
            val routes = routesForTesting(cs, ps, authCache)
            val uri = uri"/comment/create"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* create common comment */
              commonComment =
                CommentCreation(comment1.message, post.postId, comment2.commentId.some)
              commonWrongPath <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                commonComment
              )(BadRequest)
              _ <- ps.create(CreatePost(post.postId, post.message, user.userId))
              p <- pathHandlerProgram.getPath(post.postId, None)
              _ <- cs.create(CreateComment(comment2.commentId, comment2.message, user.userId, p))
              commonOk <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                commonComment
              )(Created)
            } yield expect.all(withoutAuth, withAuthWrongBody, commonWrongPath, commonOk)
        }
    }
  }

  test("update route") {
    val gen = for {
      u <- userGen
      p <- postGen
      c1 <- commentGen
      c2 <- commentGen
    } yield (u, p, c1, c2)

    forall(gen) {
      case (user, post, comment1, comment2) =>
        resourceStorages.use {
          case (_, ps, cs, ts, authCache, ac) =>
            val pathHandlerProgram = PathHandlerProgram.make(cs)
            val routes = routesForTesting(cs, ps, authCache)
            val uri = uri"/comment/update"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* update common comment */
              _ <- ps.create(CreatePost(post.postId, post.message, user.userId))
              p <- pathHandlerProgram.getPath(post.postId, None)
              _ <- cs.create(CreateComment(comment2.commentId, comment2.message, user.userId, p))
              p1 <- pathHandlerProgram.getPath(post.postId, comment2.commentId.some)
              _ <- cs.create(CreateComment(comment1.commentId, comment1.message, post.userId, p1))
              updateNotMyComment = CommentChanging(comment1.commentId, comment2.message)
              updateMyComment = CommentChanging(comment2.commentId, comment1.message)
              commonNotOk <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updateNotMyComment
              )(BadRequest)
              commonOk <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updateMyComment
              )(Ok)
              /* */
              updateNotExisting = CommentChanging(CommentId(UUID.randomUUID()), comment2.message)
              notExist <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updateNotExisting
              )(BadRequest)
            } yield expect.all(withoutAuth, withAuthWrongBody, commonNotOk, commonOk, notExist)
        }
    }
  }

  test("delete route") {
    val gen = for {
      u <- userGen
      p <- postGen
      c1 <- commentGen
      c2 <- commentGen
    } yield (u, p, c1, c2)

    forall(gen) {
      case (user, post, comment1, comment2) =>
        resourceStorages.use {
          case (_, ps, cs, ts, authCache, ac) =>
            val pathHandlerProgram = PathHandlerProgram.make(cs)
            val routes = routesForTesting(cs, ps, authCache)
            val uri = uri"/comment/delete"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* update common comment */
              _ <- ps.create(CreatePost(post.postId, post.message, user.userId))
              p <- pathHandlerProgram.getPath(post.postId, None)
              _ <- cs.create(CreateComment(comment2.commentId, comment2.message, user.userId, p))
              p1 <- pathHandlerProgram.getPath(post.postId, comment2.commentId.some)
              _ <- cs.create(CreateComment(comment1.commentId, comment1.message, post.userId, p1))
              deleteNotMyComment = CommentRemoving(comment1.commentId)
              deleteMyComment = CommentRemoving(comment2.commentId)
              commonNotOk <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                deleteNotMyComment
              )(BadRequest)
              commonOk <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                deleteMyComment
              )(Ok)
              /* */
              deleteNotExisting = CommentRemoving(CommentId(UUID.randomUUID()))
              notExist <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                deleteNotExisting
              )(BadRequest)
            } yield expect.all(withoutAuth, withAuthWrongBody, commonNotOk, commonOk, notExist)
        }
    }
  }
}
