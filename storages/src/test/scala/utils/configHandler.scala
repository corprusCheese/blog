package utils

import blog.config._
import cats.effect.{IO, Resource}
import cats.effect._
import cats.syntax.all._

object configHandler {
  val testConfig: Resource[IO, types.AppConfig] = config.makeForTest[IO]
}
