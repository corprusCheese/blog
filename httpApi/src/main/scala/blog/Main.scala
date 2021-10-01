package blog

import blog.programs.HttpProgram
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    HttpProgram.run[IO]
}
