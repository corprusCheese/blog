import blog.domain._
import blog.domain.posts._
import blog.domain.tags._
import blog.domain.users._
import blog.impl._
import cats.effect._
import cats.syntax.all._
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment._
import doobie.util.transactor.Transactor.Aux
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import suite.CustomWeaverSuite
import utils.generators._

object PostgresTest extends CustomWeaverSuite {

  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]
  val pageOk: Page = 0
  val pageFalse: Page = 110
  val perPage: PerPage = 1000

  val flushTables: List[Fragment] =
    List("users", "posts", "comments", "tags", "posts_tags").map { table =>
      Fragment.apply(s"DELETE FROM ${table};", List.empty)
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
            "jdbc:postgresql://0.0.0.0:5432/blog",
            "admin",
            "password"
          )
        ),
      tx =>
        flushTables
          .map(_.update.run.transact(tx))
          .sequence
          .map(_ => tx)
    )

  test("users") { postgres =>
    forall(userGen) { user =>
      {
        val userStorage = UserStorage.resource[IO](postgres)

        userStorage.use(us =>
          for {
            _ <- us.create(UserCreate(user.uuid, user.username, user.password))
            o <- us.findById(user.uuid)
            y <- us.fetchAll
            _ <- us.update(UserUpdate(user.uuid, user.username, user.password))
            z <- us.findById(user.uuid)
            _ <- us.delete(UserDelete(user.uuid))
            q <- us.findByName(user.username)
          } yield expect.all(
            o.nonEmpty,
            y.nonEmpty,
            z.nonEmpty,
            q.nonEmpty && q.head.deleted
          )
        )
      }
    }
  }

  test("posts") { postgres =>
    val gen = for {
      u <- userGen
      p <- postGen
    } yield (u, p)

    forall(gen) {
      case (user, post) =>
        val postStorage = PostStorage.resource[IO](postgres)
        val userStorage = UserStorage.resource[IO](postgres)

        postStorage.use(ps =>
          for {
            _ <- userStorage.use(us =>
              us.create(
                UserCreate(
                  post.userId,
                  user.username,
                  user.password
                )
              )
            )
            _ <- ps.create(CreatePost(post.postId, post.message, post.userId))
            y <- ps.getAllUserPosts(post.userId)
            x <- ps.findById(post.postId)
            z <- ps.fetchForPagination(pageOk, perPage)
            _ <- ps.update(UpdatePost(post.postId, post.message))
            u <- ps.fetchForPagination(pageFalse, perPage)
            _ <- ps.delete(DeletePost(post.postId))
            q <- ps.findById(post.postId)
          } yield expect.all(
            x.nonEmpty,
            y.nonEmpty,
            z.nonEmpty,
            u.isEmpty,
            q.nonEmpty && q.head.deleted
          )
        )
    }
  }

  test("tags") {
    { postgres =>
      val gen = for {
        u <- userGen
        p <- postGen
        t <- tagGen
      } yield (u, p, t)

      forall(gen) {
        case (user, post, tag) =>
          val postStorage = PostStorage.resource[IO](postgres)
          val userStorage = UserStorage.resource[IO](postgres)
          val tagStorage = TagStorage.resource[IO](postgres)

          tagStorage.use(ts =>
            for {
              _ <- postStorage.use(ps =>
                userStorage.use(us =>
                  us.create(
                      UserCreate(post.userId, user.username, user.password)
                    )
                    .flatMap(_ =>
                      ps.create(
                        CreatePost(post.postId, post.message, post.userId)
                      )
                    )
                )
              )
              _ <- ts.create(TagCreate(tag.tagId, tag.name, Vector.empty))
              x <- ts.findById(tag.tagId)
              y <- ts.getAllPostTags(post.postId)
              _ <- ts.update(TagUpdate(tag.tagId, tag.name, Vector(post.postId)))
              z <- ts.getAllPostTags(post.postId)
              q <- ts.fetchAll
              _ <- ts.delete(TagDelete(tag.tagId))
              p <- ts.findById(tag.tagId)
              h <- postStorage.use(ps =>
                ps.getPostsWithTagsWithPagination(tag.tagId, pageOk, perPage)
              )
            } yield expect.all(
              x.nonEmpty,
              y.isEmpty,
              z.nonEmpty,
              q.nonEmpty,
              p.nonEmpty && p.map(_.deleted == true).get,
              h.isEmpty
            )
          )
      }
    }
  }
}
