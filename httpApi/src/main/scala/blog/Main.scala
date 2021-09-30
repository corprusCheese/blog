package blog

import blog.programs.HttpProgram
import cats.effect.{ExitCode, IO, IOApp}
import dev.profunktor.redis4cats.log4cats.log4CatsInstance
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]
    HttpProgram.run[IO]
  }
}
