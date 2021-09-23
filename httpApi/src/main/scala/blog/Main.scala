package blog

import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits.catsSyntaxApplicativeId

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    println("working").pure[IO].as(ExitCode.Success)
}
