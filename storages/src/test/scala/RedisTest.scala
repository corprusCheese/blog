import cats.effect._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import eu.timepit.refined.cats._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import utils.generators.{postGen, userGen}
import weaver.IOSuite
import weaver.scalacheck.Checkers

object RedisTest extends IOSuite with Checkers {
  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://0.0.0.0")
      .evalTap(_.flushAll)

  test("authentication") {
    redis => {
      val gen = for {
        u <- userGen
      } yield (u)

      forall(gen) {
        ???
      }
    }
  }

}
