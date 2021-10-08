package api.suites

import cats.effect.IO
import cats.implicits._
import fs2.{Pure, Stream}
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
          .flatTap(res =>
            if (!res) {
              println(resp.status).pure[IO]
            } else IO.unit
          )
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

  def streamWithBody[A: Encoder](body: A): Stream[IO, Byte] =
    Stream.emits[IO, Byte](body.asJson.toString.getBytes)

  def expectFromQuery[A: Encoder, B: Decoder](
      routes: HttpRoutes[IO],
      req: Request[IO],
      body: A
  ): IO[Option[B]] = {
    routes.run(req.withBodyStream(streamWithBody(body))).value.flatMap {
      case Some(resp) =>
        resp.asJson.map(_.as[B]).flatMap(IO.fromEither(_).some.sequence)
      case None => none[B].pure[IO]
    }
  }

  def expectHttpStatusFromQuery[A: Encoder](
      routes: HttpRoutes[IO],
      req: Request[IO],
      body: A
  )(
      expectedStatus: Status
  ): IO[Boolean] =
    routes.run(req.withBodyStream(streamWithBody(body))).value.flatMap {
      case Some(resp) =>
        IO.pure(resp.status == expectedStatus)
          .flatTap(res => if (!res) resp.asJson.map( x =>println(resp.status, x)) else IO.unit)
      case None =>
        IO.pure(println(s"${req.uri} route not found")).as(false)
    }
}
