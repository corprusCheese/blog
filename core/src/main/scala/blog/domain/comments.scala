package blog.domain

import derevo.cats.{eqv, show}
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.circe.Encoder
import io.circe.syntax._

object comments {
  sealed abstract class CustomComment {
    def commentId: CommentId
    def userId: UserId
    def path: CommentMaterializedPath
  }

  implicit val customCommentEncoder: Encoder[CustomComment] = {
    case c: Comment => c.asJson
    case c: DeletedComment => c.asJson
  }

  @derive(decoder, encoder, eqv, show)
  case class Comment(
      commentId: CommentId,
      message: CommentMessage,
      userId: UserId,
      path: CommentMaterializedPath
  ) extends CustomComment

  @derive(decoder, encoder, eqv, show)
  case class DeletedComment(
      commentId: CommentId,
      userId: UserId,
      path: CommentMaterializedPath
  ) extends CustomComment

  case class CreateComment(
      commentId: CommentId,
      message: CommentMessage,
      userId: UserId,
      path: CommentMaterializedPath
  )

  case class UpdateComment(commentId: CommentId, message: CommentMessage)

  case class DeleteComment(commentId: CommentId)

}
