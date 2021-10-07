package api.suites

import blog.impl.AuthCache
import blog.storage._
import cats.effect.{IO, Resource}
import impl._

abstract class TestAuth extends HttpSuite {
  type Storages = (
      UserStorageDsl[IO],
      PostStorageDsl[IO],
      CommentStorageDsl[IO],
      TagStorageDsl[IO],
      AuthCacheDsl[IO]
  )

  def resourceStorages: Resource[IO, Storages] =
    for {
      us <- TestUserStorage.resource[IO]
      ps <- TestPostStorage.resource[IO]
      cs <- TestCommentStorage.resource[IO]
      ts <- TestTagStorage.resource[IO]
      ac <- TestAuthCache.resource[IO]
    } yield (us, ps, cs, ts, ac)
}
