package api.suites

import blog.storage.{CommentStorageDsl, PostStorageDsl, TagStorageDsl, UserStorageDsl}
import cats.effect.{IO, Resource}
import impl.{TestCommentStorage, TestPostStorage, TestTagStorage, TestUserStorage}

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
