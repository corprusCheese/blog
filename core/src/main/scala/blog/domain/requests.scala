package blog.domain

import cats.data.NonEmptyVector
import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

object requests {
  // json requests
  @derive(decoder, encoder)
  case class LoginUser(
      username: Username,
      password: Password
  )

  // posts
  @derive(decoder, encoder)
  case class PostCreation(
      message: PostMessage,
      tagIds: Option[NonEmptyVector[TagId]]
  )

  @derive(decoder, encoder)
  case class PostChanging(
      postId: PostId,
      message: PostMessage,
      tagIds: Option[NonEmptyVector[TagId]]
  )

  @derive(decoder, encoder)
  case class PostRemoving(postId: PostId)

  // tags

  @derive(decoder, encoder)
  case class TagCreation(name: TagName, postIds: Option[NonEmptyVector[PostId]])

  @derive(decoder, encoder)
  case class TagChanging(
      tagId: TagId,
      name: TagName,
      postIds: Option[NonEmptyVector[PostId]]
  )

  @derive(decoder, encoder)
  case class TagRemoving(tagId: TagId)

  // comments

  @derive(decoder, encoder)
  case class CommentCreation(
      message: CommentMessage,
      postId: PostId,
      commentId: Option[CommentId]
  )

  @derive(decoder, encoder)
  case class CommentChanging(
      commentId: CommentId,
      message: CommentMessage
  )

  @derive(decoder, encoder)
  case class CommentRemoving(commentId: CommentId)

  // fetching

  @derive(decoder, encoder)
  case class PostFilteredByTags(tagIds: NonEmptyVector[TagId])

}
