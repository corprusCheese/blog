package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

object tags {
  @derive(decoder, encoder, eqv, show)
  case class Tag(
      tagId: TagId,
      name: TagName,
      deleted: Deleted = false
  )

  case class TagCreate(
      tagId: TagId,
      name: TagName,
      postsId: Vector[PostId] = Vector.empty
  )

  case class TagUpdate(
      tagId: TagId,
      name: TagName,
      postsId: Vector[PostId] = Vector.empty
  )

  case class TagDelete(tagId: TagId)
}
