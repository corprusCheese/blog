package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

object comments {

  @derive(decoder, encoder, eqv, show)
  case class Comment(
      uuid: CommentId,
      message: CommentMessage,
      userId: UserId,
      path: CommentMaterializedPath,
      deleted: Deleted
  )

  case class CreateComment(
      messageComment: CommentMessage,
      userId: UserId,
      path: CommentMaterializedPath
  )

  case class UpdateComment(uuid: CommentId, messageComment: MessageComment)

  case class DeleteComment(commentId: CommentId)
}
