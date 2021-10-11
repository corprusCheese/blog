package blog

import blog.config.AppConfig
import blog.programs.HttpProgram
import cats.effect.{ExitCode, IO, IOApp}

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    AppConfig.make[IO].use(HttpProgram.run[IO])
}
