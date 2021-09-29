import blog.domain.users.UserCreate
import blog.impl.{AuthCommands, TokenManager, UserStorage}
import cats.effect._
import cats.implicits.catsSyntaxApplicativeId
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import eu.timepit.refined.cats._
import impl.TestUserStorage
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import utils.generators.{postGen, userGen}
import weaver.IOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.duration.DurationInt

object RedisTest extends IOSuite with Checkers {
  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
  private val expire = 30.seconds

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://0.0.0.0")
      .evalTap(_.flushAll)

  test("authentication") { redis =>
    {
      val gen = for {
        u1 <- userGen
        u2 <- userGen
      } yield (u1, u2)

      forall(gen) {
        case (user1, user2) =>
          TokenManager
            .resource(expire)
            .use(tm =>
              TestUserStorage.resource.use(us => {
                for {
                  _ <- tm.create
                  ac = AuthCommands.make(redis, us, tm, expire)
                  x <- ac.newUser(user1.uuid, user1.username, user1.password)
                  y <- us.findByName(user1.username)
                  z <- ac.login(user1.username, user2.password)
                  q <- ac.login(user1.username, user1.password)
                  w <- ac.login(user2.username, user2.password)
                  _ <- ac.logout(q.get, user1.uuid)
                } yield expect.all(
                  x.nonEmpty,
                  y.nonEmpty,
                  z.isEmpty,
                  q.nonEmpty,
                  w.isEmpty
                )
              })
            )
      }
    }
  }

}
