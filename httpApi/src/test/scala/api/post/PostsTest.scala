package api.post

import api.suites.TestCommon
import blog.programs.PostProgram
import blog.routes.Posts
import blog.storage.{CommentStorageDsl, PostStorageDsl, TagStorageDsl}
import cats.effect.IO
import org.http4s.HttpRoutes

object PostsTest extends TestCommon {
  private def routesForTesting(
      ps: PostStorageDsl[IO],
      cs: CommentStorageDsl[IO],
      ts: TagStorageDsl[IO]
  ): HttpRoutes[IO] = Posts[IO](PostProgram.make(ps, cs, ts)).routesWithoutAuthOnly
}
