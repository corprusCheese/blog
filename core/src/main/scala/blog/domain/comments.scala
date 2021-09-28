package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

object comments {

  @derive(decoder, encoder, eqv, show)
  case class Comment(
      commentId: CommentId,
      message: CommentMessage,
      userId: UserId,
      path: CommentMaterializedPath,
      deleted: Deleted = false
  )

  case class CreateComment(
      commentId: CommentId,
      message: CommentMessage,
      userId: UserId,
      path: CommentMaterializedPath
  )

  case class UpdateComment(commentId: CommentId, message: CommentMessage)

  case class DeleteComment(commentId: CommentId)

}
