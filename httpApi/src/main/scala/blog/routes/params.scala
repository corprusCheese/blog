package blog.routes

import blog.domain._
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.types.string.NonEmptyString
import org.http4s.dsl.impl.UUIDVar

object params {
  object PostIdVar{
    def unapply(str: String): Option[PostId] = {
      UUIDVar.unapply(str) match {
        case Some(v) => PostId.apply(v).some
        case None => None
      }
    }
  }

  object TagIdVar{
    def unapply(str: String): Option[TagId] = {
      UUIDVar.unapply(str) match {
        case Some(v) => TagId.apply(v).some
        case None => None
      }
    }
  }

  object TagNameVar{
    def unapply(str: String): Option[TagName] = {
      NonEmptyString.unapply(str) match {
        case Some(v) => TagName(v).some
        case None => None
      }
    }
  }

  object CommentIdVar{
    def unapply(str: String): Option[CommentId] = {
      UUIDVar.unapply(str) match {
        case Some(v) => CommentId.apply(v).some
        case None => None
      }
    }
  }

  object UserIdVar{
    def unapply(str: String): Option[UserId] = {
      UUIDVar.unapply(str) match {
        case Some(v) => UserId.apply(v).some
        case None => None
      }
    }
  }
}
