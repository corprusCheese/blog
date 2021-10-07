package api.post

import api.suites.TestAuth
import blog.config.JwtSecretKey
import blog.domain.users.User
import blog.middlewares.commonAuthMiddleware
import blog.programs.PostProgram
import blog.routes.Posts
import blog.storage.{AuthCacheDsl, CommentStorageDsl, PostStorageDsl, TagStorageDsl}
import cats.effect.IO
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.HttpRoutes
import org.http4s.server.AuthMiddleware

object PostsTest extends TestAuth {
  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO],
      ac: AuthCacheDsl[IO]
  ): HttpRoutes[IO] = {
    val jwtAuth: JwtSecretKey = JwtSecretKey.apply(NonEmptyString.unsafeFrom("my very secret test password"))
    val authMiddleware: AuthMiddleware[F, User] = commonAuthMiddleware(ac, jwtAuth)
    Posts[IO](PostProgram.make(ps, cs, ts)).routesWithAuthOnly(authMiddleware)
  }
}
