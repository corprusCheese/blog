package utils

import cats.effect.{IO, MonadCancelThrow}
import doobie.util.fragment.Fragment
import doobie.util.transactor.Transactor
import doobie.implicits._
import cats.syntax.all._

object postgres {
  private val flushTables: List[Fragment] =
    List("posts_tags", "tags", "comments", "posts", "users").map { table =>
      Fragment.apply(s"DELETE FROM ${table};", List.empty)
    }

  def deleteAll[F[_]: MonadCancelThrow](tx: Transactor[F]): F[Unit] =
    flushTables
      .map(_.update.run.transact(tx))
      .sequence
      .map(_ => tx)
}
