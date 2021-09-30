package blog

import scala.util.control.NoStackTrace

object errors {
  sealed abstract class CustomError(val msg: String) extends NoStackTrace

  case object LoginError extends CustomError("Credentials are not right")
  case object RegisterError extends CustomError("User with such username already exists")

  case object CreatePostError extends CustomError("Can't create post")
  case object UpdatePostError extends CustomError("Can't update post")
  case object DeletePostError extends CustomError("Can't delete post")
  case object DeletePostCommentError extends CustomError("Can't delete comments when deleting post")

  case object NoPostsFromUser extends CustomError("There is no post from this user")
  case object NoPostsWithTag extends CustomError("There is no posts with this tag")

}
