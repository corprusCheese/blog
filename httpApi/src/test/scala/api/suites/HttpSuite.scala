package api.suites

import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import io.circe._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import weaver._
import weaver.scalacheck.Checkers

trait HttpSuite extends SimpleIOSuite with Checkers {

  def expectHttpBodyAndStatus[A: Encoder](
      routes: HttpRoutes[IO],
      req: Request[IO]
  )(
      expectedBody: A,
      expectedStatus: Status
  ): IO[Boolean] =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        resp.asJson.map { json =>
          resp.status == expectedStatus && json == expectedBody.asJson
        }
      case None => IO.pure(println(s" ${req.uri} route not found")).as(false)
    }

  def expectHttpStatus(routes: HttpRoutes[IO], req: Request[IO])(
      expectedStatus: Status
  ): IO[Boolean] =
    routes.run(req).value.flatMap {
      case Some(resp) =>
        IO.pure(resp.status == expectedStatus)
          .flatTap(res => if (!res) println(resp.status).pure[IO] else IO.unit)
      case None =>
        IO.pure(println(s"${req.uri} route not found")).as(false)
    }

  def expectHttpFailure(
      routes: HttpRoutes[IO],
      req: Request[IO]
  ): IO[Boolean] =
    routes.run(req).value.attempt.map {
      case Left(_)  => true
      case Right(_) => false
    }
}
