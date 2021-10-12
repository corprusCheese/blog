package blog.utils.routes

import cats.data.NonEmptyVector
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import org.http4s.QueryParamDecoder
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

object params {
  def getVectorFromOptionNev[A](value: Option[NonEmptyVector[A]]): Vector[A] =
    value match {
      case None     => Vector.empty[A]
      case Some(in) => in.toVector.distinct
    }

  abstract class OptionalQueryParamDecoderMatcherWithDefault[
      T: QueryParamDecoder
  ](name: String, default: T)
      extends QueryParamDecoderMatcher[T](name) {
    override def unapply(
        params: Map[String, collection.Seq[String]]
    ): Option[T] = {
      super.unapply(params).orElse(Some(default))
    }
  }

}
