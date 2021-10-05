package unit.utils

import blog.utils.ext.refined.RefinedRequestDecoder
import cats.effect.IO
import cats.implicits._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import ext.RefinedClass
import fs2.Stream
import io.circe.syntax.EncoderOps
import org.http4s.Method._
import org.http4s._
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

object RefinedExtTest extends SimpleIOSuite with Checkers {

  test("can decode entity with refined types") {
    val str: String = "nonemptystring"
    val refined: RefinedClass = RefinedClass(NonEmptyString.unsafeFrom(str))

    val s = refined.asJson.toString.getBytes
    val request: Request[IO] =
      Request[IO](POST, Uri()).withBodyStream(Stream.emits(s))

    request
      .decodeR[RefinedClass] { decoder =>
        {
          (decoder == refined)
            .pure[IO]
            .map(answer => Response.apply[IO](Status(200, if (answer) "OK" else "NO")))
        }
      }
      .map(resp => {
        assert(resp.status.reason == "OK")
      })
  }
}
