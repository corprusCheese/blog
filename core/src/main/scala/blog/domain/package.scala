package blog

import derevo.cats._
import derevo.circe.magnolia._
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.cats._
import eu.timepit.refined.numeric._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._
import io.estatico.newtype.macros.newtype

import java.util.UUID

package object domain {

  // not refined
  type Deleted = Boolean
  type LTree = String

  // refined
  type Pass = NonEmptyString
  type HashedPass = NonEmptyString
  type MessageComment = NonEmptyString
  type MessagePost = NonEmptyString
  type Name = NonEmptyString
  type Page = Int Refined NonNegative
  type PerPage = Int Refined NonNegative

  // without refined
  @derive(encoder, decoder, eqv, show)
  @newtype
  case class UserId(value: UUID)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class Username(value: Name)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class Password(value: Pass)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class HashedPassword(value: HashedPass)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class CommentId(value: UUID)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class CommentMessage(value: MessageComment)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class CommentMaterializedPath(value: LTree)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class PostId(value: UUID)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class PostMessage(value: MessagePost)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class TagId(value: UUID)

  @derive(encoder, decoder, eqv, show)
  @newtype
  case class TagName(value: NonEmptyString)

}
