package unit.utils

import cats.effect.{IO, MonadCancelThrow, Resource}
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import doobie.implicits._
import cats.syntax.all._
import doobie.util.transactor.Transactor.Aux

object pgs {
  private val flushTables: List[Fragment] =
    List("posts_tags", "tags", "comments", "posts", "users").map { table =>
      Fragment.apply(s"DELETE FROM ${table};", List.empty)
    }

  def deleteAll[F[_]: MonadCancelThrow](tx: Transactor[F]): F[Unit] =
    flushTables
      .map(_.update.run.transact(tx))
      .sequence
      .map(_ => tx)

  def transactor: Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql://0.0.0.0:5433/blog",
      "admin",
      "password"
    )
}
