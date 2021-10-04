package blog

import blog.config.config
import blog.programs.HttpProgram
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    config.make[IO].use(HttpProgram.run[IO])
}
