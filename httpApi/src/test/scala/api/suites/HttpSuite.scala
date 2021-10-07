package api.suites

import cats.effect.IO
import io.circe._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import weaver._
import weaver.scalacheck.Checkers

trait HttpSuite extends IOSuite with Checkers {

  def expectHttpBodyAndStatus[A: Encoder](
      routes: HttpRoutes[IO],
      req: Request[IO]
  )(
      expectedBody: A,
      expectedStatus: Status
  ): IO[Expectations] =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.asJson.map { json =>
          {
            expect.all(
              resp.status == expectedStatus,
              json == expectedBody.asJson
            )
          }
        }
      case None => IO.pure(failure(s" ${req.uri} route not found"))
    }

  def expectHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: Status
  ): IO[Expectations] =
    routes.run(req).value.map {
      case Some(resp) => expect.same(resp.status, expectedStatus)
      case None       => failure(s"${req.uri} route not found")
    }

  def expectHttpFailure(
      routes: HttpRoutes[IO],
      req: Request[IO]
  ): IO[Expectations] =
    routes.run(req).value.attempt.map {
      case Left(_)  => success
      case Right(_) => failure("expected a failure")
    }
}
