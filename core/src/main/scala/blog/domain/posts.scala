package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

object posts {
  @derive(decoder, encoder, eqv, show)
  case class Post(
      postId: PostId,
      message: PostMessage,
      userId: UserId
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

  @derive(decoder, encoder, eqv, show)
  case class DeletePost(postId: PostId)
}
