package blog

import scala.util.control.NoStackTrace

object errors {
  sealed trait CustomError extends NoStackTrace

  case class LoginError(msg: String) extends CustomError
  case class RegisterError(msg: String) extends CustomError

}
