import blog.impl.{AuthCommands, TokenManager}
import cats.effect._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import impl.TestUserStorage
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import utils.configHandler.testConfig
import utils.generators.userGen
import weaver.IOSuite
import weaver.scalacheck.Checkers

object RedisTest extends IOSuite with Checkers {

  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    testConfig.flatMap(config => {
      Redis[IO]
        .utf8(config.redisConfig.uri)
        .evalTap(_.flushAll)
    })

  test("authentication") { redis =>
    {
      val gen = for {
        u1 <- userGen
        u2 <- userGen
      } yield (u1, u2)

      forall(gen) {
        case (user1, user2) =>
          testConfig.use(config =>
            TokenManager
              .resource(config.tokenExpiration, config.jwtAccessTokenKey)
              .use(tm =>
                TestUserStorage.resource.use(us => {
                  for {
                    _ <- tm.create
                    ac =
                      AuthCommands.make(redis, us, tm, config.tokenExpiration)
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
          )
      }
    }
  }
}
