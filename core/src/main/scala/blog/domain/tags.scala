package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

object tags {
  @derive(decoder, encoder, eqv, show)
  case class Tag(uuid: TagId, name: TagName, deleted: Deleted)

  case class CreateTag(name: TagName)

  case class UpdateTag(uuid: TagId, name: TagName)

  case class DeleteTag(tagId: TagId)
}
