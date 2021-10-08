package blog.utils.routes

import blog.domain.Page
import cats.data.NonEmptyVector
import eu.timepit.refined.types.numeric.NonNegInt
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import org.http4s.Request

object params {
  def getPage[F[_]](req: Request[F]): Page =
    try { NonNegInt.unsafeFrom(req.params("page").toInt) }
    catch { case _: Throwable => 0 }

  def getVectorFromOptionNev[A](value: Option[NonEmptyVector[A]]): Vector[A] =
    value match {
      case None => Vector.empty[A]
      case Some(in) => in.toVector.distinct
    }
}
