package api.post

import api.suites.TestAuth
import blog.config.JwtSecretKey
import blog.domain.TagId
import blog.domain.posts.CreatePost
import blog.domain.requests.{PostChanging, PostCreation, PostRemoving}
import blog.domain.tags.TagCreate
import blog.middlewares.commonAuthMiddleware
import blog.programs.PostProgram
import blog.routes.Posts
import blog.storage._
import cats.data.NonEmptyVector
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import gen.generators._
import org.http4s.HttpRoutes
import org.http4s.Status._
import org.http4s.syntax.literals._

object PostsTest extends TestAuth {
  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO],
      ac: AuthCacheDsl[IO]
  ): HttpRoutes[IO] =
    Posts[IO](PostProgram.make(ps, cs, ts)).routesWithAuthOnly(
      commonAuthMiddleware(
        ac,
        JwtSecretKey.apply(
          NonEmptyString.unsafeFrom("secret")
        )
      )
    )

  test("create route") {
    val gen = for {
      u <- userGen
      p1 <- postGen
      p2 <- postGen
      t <- tagGen
    } yield (u, p1, p2, t)

    forall(gen) {
      case (user, post1, post2, tag) =>
        resourceStorages.use {
          case (_, ps, cs, ts, authCache, ac) =>
            val routes = routesForTesting(ps, cs, ts, authCache)
            val uri = uri"/post/create"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* use right format for query  */
              withAuthOkBody <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                post1
              )(Created)
              /* use wrong tag ids - it does not exist now */
              updatedPost2 = PostCreation(
                post2.message,
                NonEmptyVector.one(tag.tagId).some
              )
              notExistingTags <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost2
              )(BadRequest)
              /* tag exists now - server should return Created */
              _ <- ts.create(TagCreate(tag.tagId, tag.name))
              existingTags <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost2
              )(Created)
            } yield expect.all(
              withoutAuth,
              withAuthWrongBody,
              withAuthOkBody,
              notExistingTags,
              existingTags
            )
        }
    }
  }

  test("update route") {
    val gen = for {
      u <- userGen
      p1 <- postGen
      p2 <- postGen
      t <- tagGen
    } yield (u, p1, p2, t)

    forall(gen) {
      case (user, post1, post2, tag) =>
        resourceStorages.use {
          case (_, ps, cs, ts, authCache, ac) =>
            val routes = routesForTesting(ps, cs, ts, authCache)
            val uri = uri"/post/update"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              _ <-
                ps.create(CreatePost(post1.postId, post1.message, post1.userId))
              updatedPost1 = PostChanging(
                post1.postId,
                post2.message,
                none[NonEmptyVector[TagId]]
              )
              /* Post don't belong to user */
              changeMessageFail <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost1
              )(BadRequest)
              /* post created by auth user */
              updatedPost2 = PostChanging(
                post2.postId,
                post1.message,
                none[NonEmptyVector[TagId]]
              )
              changeMessageNoSuchId <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost2
              )(BadRequest)
              _ <-
                ps.create(CreatePost(post2.postId, post2.message, user.userId))
              changeMessageOk <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost2
              )(Ok)
              /* tags testing */
              updatedPost3 = PostChanging(
                post2.postId,
                post1.message,
                NonEmptyVector.one(tag.tagId).some
              )
              noTags <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost3
              )(BadRequest)
              /* tag exists now - server should return Ok */
              _ <- ts.create(TagCreate(tag.tagId, tag.name))
              existingTags <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                updatedPost3
              )(Ok)
            } yield expect.all(
              withoutAuth,
              withAuthWrongBody,
              changeMessageFail,
              changeMessageOk,
              noTags,
              existingTags
            )
        }
    }
  }

  test("delete route") {
    val gen = for {
      u <- userGen
      p1 <- postGen
      p2 <- postGen
      t <- tagGen
    } yield (u, p1, p2, t)

    forall(gen) {
      case (user, post1, post2, tag) =>
        resourceStorages.use {
          case (_, ps, cs, ts, authCache, ac) =>
            val routes = routesForTesting(ps, cs, ts, authCache)
            val uri = uri"/post/delete"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* you can delete only posts that you create */
              _ <-
                ps.create(CreatePost(post1.postId, post1.message, post1.userId))
              _ <-
                ps.create(CreatePost(post2.postId, post2.message, user.userId))
              deleteNotMyPost = PostRemoving(post1.postId)
              deleteMyPost = PostRemoving(post2.postId)
              noDeleting <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                deleteNotMyPost
              )(BadRequest)
              deleting <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                deleteMyPost
              )(Ok)
            } yield expect.all(
              withoutAuth,
              withAuthWrongBody,
              noDeleting,
              deleting
            )
        }
    }
  }
}
