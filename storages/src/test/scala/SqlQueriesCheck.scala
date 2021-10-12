import blog.domain.tags.TagCreate
import blog.impl._
import blog.queries.tagQueries
import cats.effect._
import cats.implicits.catsSyntaxApplicativeId
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import gen.generators._
import org.scalatest.matchers.must.Matchers
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import unit.utils.pgs
import weaver.{Expectations, SimpleIOSuite}
import weaver.scalacheck.Checkers

object SqlQueriesCheck extends SimpleIOSuite with Matchers with Checkers {
  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  val tx: doobie.Transactor[IO] = pgs.transactor
  val yolo = tx.yolo
  import yolo._

  test("sql analysis") {
    val gen = for {
      t <- tagGen
      p <- postGen
    } yield (t, p)

    forall(gen) {
      case (tag, post) =>
        for {
          _ <- tagQueries.queryForFindById(tag.tagId).check
          _ <- tagQueries.queryForCreateTag(TagCreate(tag.tagId, tag.name)).check
          _ <- tagQueries.queryForFetchAll.check
          _ <- tagQueries.queryForGetPostTags(post.postId).check
          _ <- tagQueries.queryForFindByName(tag.name).check
          _ <- tagQueries.queryForUpdateBoundTable(tag.tagId, post.postId).check
        } yield expect.all()
    }
  }
}
