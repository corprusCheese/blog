package api.get

import api.suites.TestCommon
import blog.domain.comments._
import blog.programs._
import blog.programs.comment.PathHandlerProgram
import blog.routes._
import blog.storage._
import cats.effect.IO
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import gen.generators._
import org.http4s._
import org.http4s.Method.GET
import org.http4s.Status.{NotFound, Ok}
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._

/**
  * test with forall using
  */
object CommentsTest extends TestCommon {

  private def routesForTesting(
      cs: CommentStorageDsl[IO]
  ): HttpRoutes[IO] =
    Comments[IO](CommentProgram.make(cs)).routesWithoutAuthOnly

  test("fetch all") {
    val gen = for {
      c1 <- commentGen
      c2 <- commentGen
    } yield (c1, c2)

    forall(gen) {
      case (comment1, comment2) =>
        resourceStorages.use {
          case (_, _, cs, _) =>
            for {
              _ <- cs.create(
                CreateComment(
                  comment1.commentId,
                  comment1.message,
                  comment1.userId,
                  comment1.path
                )
              )
              _ <- cs.create(
                CreateComment(
                  comment2.commentId,
                  comment2.message,
                  comment2.userId,
                  comment2.path
                )
              )
              expectedBody <- cs.fetchAll
              creatingComment <- expectHttpBodyAndStatus(
                routesForTesting(cs),
                GET(uri"/comment/all")
              )(expectedBody, Ok)
              _ <- cs.delete(DeleteComment(comment1.commentId))
              _ <- cs.delete(DeleteComment(comment2.commentId))
              deletingAllComments <- expectHttpStatus(
                routesForTesting(cs),
                GET(uri"/comment/all")
              )(NotFound)
            } yield expect.all(creatingComment, deletingAllComments)
        }
    }
  }

  test("get all comments of user") {
    val gen = for {
      c1 <- commentGen
      c2 <- commentGen
    } yield (c1, c2)

    forall(gen) {
      case (comment1, comment2) =>
        resourceStorages.use {
          case (_, _, cs, _) =>
            for {
              _ <- cs.create(
                CreateComment(
                  comment1.commentId,
                  comment1.message,
                  comment1.userId,
                  comment1.path
                )
              )
              _ <- cs.create(
                CreateComment(
                  comment2.commentId,
                  comment2.message,
                  comment1.userId,
                  comment2.path
                )
              )
              expectedBody <- cs.getActiveUserComments(comment1.userId)
              expected2Comments <- expectHttpBodyAndStatus(
                routesForTesting(cs),
                GET(Uri.unsafeFromString(s"/comment/user/${comment1.userId}"))
              )(expectedBody, Ok)
              _ <- cs.delete(DeleteComment(comment1.commentId))
              _ <- cs.delete(DeleteComment(comment2.commentId))
              expected0Comments <- expectHttpStatus(
                routesForTesting(cs),
                GET(Uri.unsafeFromString(s"/comment/user/${comment1.userId}"))
              )(NotFound)
            } yield expect.all(expected2Comments, expected0Comments)
        }
    }
  }

  test("get all comments of post") {
    val gen = for {
      c1 <- commentGen
      c2 <- commentGen
      p <- postGen
    } yield (c1, c2, p)

    forall(gen) {
      case (comment1, comment2, post) =>
        resourceStorages.use {
          case (_, _, cs, _) =>
            val pathHandlerProgram: PathHandlerProgram[IO] =
              PathHandlerProgram.make(cs)
            for {
              path1 <- pathHandlerProgram.getPath(post.postId, None)
              _ <- cs.create(
                CreateComment(
                  comment1.commentId,
                  comment1.message,
                  comment1.userId,
                  path1
                )
              )
              _ <- cs.create(
                CreateComment(
                  comment2.commentId,
                  comment2.message,
                  comment2.userId,
                  comment2.path
                )
              )
              all <- cs.fetchAll
              fromUser <- cs.getAllPostComments(post.postId)
              expectedOnly1 <- expectHttpBodyAndStatus(
                routesForTesting(cs),
                GET(Uri.unsafeFromString(s"/comment/post/${post.postId}"))
              )(fromUser, Ok)
            } yield expect.all(
              all != fromUser,
              fromUser.head.commentId == comment1.commentId,
              fromUser.size == 1,
              expectedOnly1
            )
        }
    }
  }

  test("find comment by id") {
    val gen = for {
      c1 <- commentGen
      c2 <- commentGen
      p <- postGen
    } yield (c1, c2, p)

    forall(gen) {
      case (comment1, _, _) =>
        resourceStorages.use {
          case (_, _, cs, _) =>
            for {
              e <- expectHttpStatus(
                routesForTesting(cs),
                GET(Uri.unsafeFromString(s"/comment/${comment1.commentId}"))
              )(NotFound)
              _ <- cs.create(
                CreateComment(
                  comment1.commentId,
                  comment1.message,
                  comment1.userId,
                  comment1.path
                )
              )
              e1 <- expectHttpBodyAndStatus(
                routesForTesting(cs),
                GET(Uri.unsafeFromString(s"/comment/${comment1.commentId}"))
              )(comment1, Ok)
            } yield expect.all(e, e1)
        }
    }
  }
}
