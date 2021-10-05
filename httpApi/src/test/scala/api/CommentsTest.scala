package api

import blog.routes.Comments
import cats.effect.IO
import gen.generators.{commentGen, postGen, userGen}
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object CommentsTest extends SimpleIOSuite with Checkers {
  /*test("GET all comments") {
    val gen = for {
      u <- userGen
      p <- postGen
      c <- commentGen
    } yield (u, p, c)

    forall(gen) {
      case (user, post, comment) => {
        val routes = Comments[IO]
      }
    }
  }*/
}
