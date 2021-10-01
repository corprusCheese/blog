package blog

import scala.util.control.NoStackTrace

object errors {
  sealed abstract class CustomError(val msg: String) extends NoStackTrace

  case object LoginError extends CustomError("Credentials are not right")
  case object RegisterError
      extends CustomError("User with such username already exists")

  case object CreatePostError extends CustomError("Can't create post")
  case object UpdatePostError extends CustomError("Can't update post")
  case object DeletePostError extends CustomError("Can't delete post")
  case object DeletePostCommentError
      extends CustomError("Can't delete comments when deleting post")

  case object NoPostsFromUser
      extends CustomError("There is no post from this user")
  case object NoPostsWithTag
      extends CustomError("There is no posts with this tag")

  case object CreateTagError extends CustomError("Can't create tag")
  case object UpdateTagError extends CustomError("Can't update tag")
  case object DeleteTagError extends CustomError("Can't delete tag")

  case object UpdateTagWithNotYoursPosts
      extends CustomError(
        "Can't update tag with such posts, because one of them not yours"
      )

  case object DeleteTagWithNotYoursPosts
    extends CustomError(
      "Can't update tag with such posts, because one of them not yours"
    )

  case object CreateCommentError extends CustomError("Can't create comment")
  case object UpdateCommentError extends CustomError("Can't update comment")
  case object DeleteCommentError extends CustomError("Can't delete comment")

  case object NoSuchCommentId extends CustomError("No such comment id")

  case object CommentDontBelongToUser extends CustomError("Comment don't belong to user")
  case object PostDontBelongToUser extends CustomError("Post don't belong to user")

  case object CommentNotExists extends CustomError("Such comment id not exists")
  case object PostNotExists extends CustomError("Such post id not exists")

  // it is for later, only admin can delete tags
  case object TagNotExists extends CustomError("Such tag id not exists")

}
