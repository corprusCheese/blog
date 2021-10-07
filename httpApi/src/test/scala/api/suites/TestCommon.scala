package api.suites

import blog.storage._
import cats.effect._
import impl._

abstract class TestCommon extends HttpSuite {
  override type Res = (
      UserStorageDsl[IO],
      PostStorageDsl[IO],
      CommentStorageDsl[IO],
      TagStorageDsl[IO]
  )

  override def sharedResource: Resource[IO, Res] =
    for {
      us <- TestUserStorage.resource[IO]
      ps <- TestPostStorage.resource[IO]
      cs <- TestCommentStorage.resource[IO]
      ts <- TestTagStorage.resource[IO]
    } yield (us, ps, cs, ts)
}
