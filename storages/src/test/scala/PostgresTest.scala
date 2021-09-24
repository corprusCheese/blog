import cats.effect._
import cats.syntax.all._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment
import doobie.util.transactor.Transactor.Aux
import suite.CustomWeaverSuite
import utils.generators._
import cats._
import cats.effect.implicits._
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._

object PostgresTest extends CustomWeaverSuite {

  val flushTables: List[fragment.Fragment] =
    List("users", "posts", "comments", "tags", "posts_tags").map { table =>
      sql"""DELETE FROM $table"""
    }

  override type Res = Aux[IO, Unit]

  def afterAll(res: Resource[IO, Res], f: Res => IO[Unit]): Resource[IO, Res] =
    res.flatTap(x => Resource.make(IO.unit)(_ => f(x)))

  override def sharedResource: Resource[IO, Res] =
    afterAll(
      Resource
        .pure[IO, Aux[IO, Unit]](
          Transactor.fromDriverManager[IO](
            "org.postgresql.Driver",
            "jdbc:postgresql:blog",
            "admin",
            "password"
          )
        ),
      tx =>
        flushTables
          .map(fragment => fragment.update.run.transact(tx))
          .sequence
          .map(_ => tx)
    )

  test("users") { postgres =>
    forall(userGen)
      { user => assert(1!=1)
        /*val userStorage = UserStorage.make[IO](postgres)
      userStorage.use(us =>
        for {
          x <- us.fetchAll
          _ <- us.create(UserCreate(user.username, user.password))
          y <- us.fetchAll
        } yield true
      )*/
      }
  }

  /*test("posts") { postgres =>
    forall(postGen) { post => {} }
  }*/

}
