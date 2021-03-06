package api.suites

import blog.storage._
import cats.effect._
import impl._

abstract class TestCommon extends HttpSuite {
  type Storages = (
      UserStorageDsl[IO],
      PostStorageDsl[IO],
      CommentStorageDsl[IO],
      TagStorageDsl[IO]
  )

  def resourceStorages: Resource[IO, Storages] =
    for {
      us <- TestUserStorage.resource[IO]
      ps <- TestPostStorage.resource[IO]
      cs <- TestCommentStorage.resource[IO]
      ts <- TestTagStorage.resource[IO]
    } yield (us, ps, cs, ts)
}
