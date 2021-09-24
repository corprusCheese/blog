package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

object posts {
  @derive(decoder, encoder, eqv, show)
  case class Post(uuid: PostId, message: PostMessage, userId: UserId, deleted: Deleted)

  case class CreatePost(message: PostMessage, userId: UserId)

  case class UpdatePost(uuid: PostId, message: PostMessage)

  case class DeletePost(postId: PostId)
}
