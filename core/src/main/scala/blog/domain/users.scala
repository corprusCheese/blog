package blog.domain

import derevo.cats._
import derevo.circe.magnolia._
import derevo.derive

object users {

  @derive(decoder, encoder, eqv, show)
  case class User(
      userId: UserId,
      username: Username,
      password: HashedPassword
  )

  case class UserCreate(
      userId: UserId,
      username: Username,
      password: HashedPassword
  )

  case class UserUpdate(
      userId: UserId,
      username: Username,
      password: HashedPassword
  )

  case class UserDelete(userId: UserId)
}
