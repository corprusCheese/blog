import blog.domain.users.User
import blog.impl._
import cats.effect._
import cats.syntax.all._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.effect.Log.NoOp.instance
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import gen.generators._
import io.circe.syntax.EncoderOps
import org.scalacheck.Gen
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import weaver.IOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object RedisTest extends IOSuite with Checkers {

  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
  val timeout: FiniteDuration = 30.seconds

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://0.0.0.0:6380")
      .evalTap(_.flushAll)

  test("authentication") { redis =>
    {
      val gen: Gen[(User, User, String)] = for {
        u1 <- userGen
        u2 <- userGen
        t <- tokenAsStringGen
      } yield (u1, u2, t)

      forall(gen) {
        case (user1, user2, tokenString) =>
          val token = JwtToken(tokenString)
          AuthCache
            .resource(redis)
            .use(cache =>
              for {
                _ <- cache.setToken(user1, token, timeout)
                x <- cache.getTokenAsString(user1.uuid)
                y <- cache.getTokenAsString(user2.uuid)
                z <- cache.getUserAsString(token)
                _ <- cache.delToken(user1.uuid, token)
                o <- cache.getTokenAsString(user1.uuid)
              } yield expect.all(
                x.nonEmpty && x == token.value.some,
                y.isEmpty,
                z.nonEmpty && z == user1.asJson.toString.some,
                o.isEmpty
              )
            )

      }
    }
  }
}
