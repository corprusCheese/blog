package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype
import jdk.incubator.vector.VectorOperators

object posts {
  @derive(decoder, encoder, eqv, show)
  case class Post(
      postId: PostId,
      message: PostMessage,
      userId: UserId,
      deleted: Deleted = false
  )

  case class CreatePost(
      postId: PostId,
      message: PostMessage,
      userId: UserId,
      tagsId: Vector[TagId] = Vector.empty
  )

  case class UpdatePost(
      postId: PostId,
      message: PostMessage,
      tagsId: Vector[TagId] = Vector.empty
  )

  case class DeletePost(postId: PostId)
}