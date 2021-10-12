package api.post

import api.suites.TestAuth
import blog.config.types.JwtConfigSecretKey
import blog.domain.TagId
import blog.domain.posts.CreatePost
import blog.domain.requests.{TagChanging, TagCreation}
import blog.domain.tags.TagCreate
import blog.middlewares.commonAuthMiddleware
import blog.programs.TagProgram
import blog.routes.Tags
import blog.storage.{AuthCacheDsl, PostStorageDsl, TagStorageDsl}
import cats.data.NonEmptyVector
import cats.effect.IO
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.types.string.NonEmptyString
import gen.generators.{postGen, tagGen, userGen}
import org.http4s.HttpRoutes
import org.http4s.Status.{BadRequest, Created, Ok}
import org.http4s.implicits.http4sLiteralsSyntax

import java.util.UUID

object TagsTest extends TestAuth {
  private def routesForTesting(
      ts: TagStorageDsl[IO],
      ps: PostStorageDsl[IO],
      ac: AuthCacheDsl[IO]
  ): HttpRoutes[IO] =
    Tags(TagProgram.make(ts, ps)).routesWithAuthOnly(
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
      t1 <- tagGen
      t2 <- tagGen
      p <- postGen
    } yield (u, t1, t2, p)

    forall(gen) {
      case (user, tag1, tag2, post) =>
        resourceStorages.use {
          case (_, ps, _, ts, authCache, ac) =>
            val routes = routesForTesting(ts, ps, authCache)
            val uri = uri"/tag/create"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* */
              createTagCommon = TagCreation(tag1.name, None)
              common <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                createTagCommon
              )(Created)
              createTagWithPost =
                TagCreation(tag2.name, NonEmptyVector.one(post.postId).some)
              noPost <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                createTagWithPost
              )(BadRequest)
              _ <- ps.create(
                CreatePost(post.postId, post.message, user.userId)
              ) // any user actually
              postExist <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                createTagWithPost
              )(Created)
            } yield expect.all(
              withoutAuth,
              withAuthWrongBody,
              common,
              noPost,
              postExist
            )
        }
    }
  }

  test("update route") {
    val gen = for {
      u <- userGen
      t1 <- tagGen
      t2 <- tagGen
      p1 <- postGen
      p2 <- postGen
    } yield (u, t1, t2, p1, p2)

    forall(gen) {
      case (user, tag1, tag2, post, postMy) =>
        resourceStorages.use {
          case (us, ps, cs, ts, authCache, ac) =>
            val routes = routesForTesting(ts, ps, authCache)
            val uri = uri"/tag/update"

            for {
              /* usual first step */
              cortege <- firstStep(uri, routes, user, ac)
              withoutAuth = cortege._1
              withAuthWrongBody = cortege._2
              token = cortege._3
              /* try update with wrong id and ok id */
              _ <- ts.create(TagCreate(tag1.tagId, tag1.name))
              commonUpdate = TagChanging(tag1.tagId, tag2.name, None)
              wrongIdUpdate =
                TagChanging(TagId(UUID.randomUUID()), tag1.name, None)
              common <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                commonUpdate
              )(Ok)
              wrongId <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                wrongIdUpdate
              )(BadRequest)
              /* try update with wrong post id*/
              _ <- ts.create(TagCreate(tag2.tagId, tag2.name))
              postIdUpdateNotMy = TagChanging(
                tag2.tagId,
                tag1.name,
                NonEmptyVector.one(post.postId).some
              )
              noPost <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                postIdUpdateNotMy
              )(BadRequest)
              _ <-
                ps.create(
                  CreatePost(post.postId, post.message, post.userId)
                ) // not my user
              existNotMyPost <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                postIdUpdateNotMy
              )(BadRequest)
              postIdUpdateMy = TagChanging(
                tag2.tagId,
                tag2.name,
                NonEmptyVector.one(postMy.postId).some
              )
              _ <- ps.create(
                CreatePost(postMy.postId, postMy.message, user.userId)
              ) // my user
              existMyPost <- expectHttpStatusFromQuery(
                routes,
                POST_AUTH(uri, token),
                postIdUpdateMy
              )(Ok)
            } yield expect.all(
              withoutAuth,
              withAuthWrongBody,
              common,
              wrongId,
              noPost,
              existNotMyPost,
              existMyPost
            )
        }
    }
  }
}
