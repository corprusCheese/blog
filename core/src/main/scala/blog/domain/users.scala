package blog.domain

import derevo.cats._
import derevo.circe.magnolia._
import derevo.derive
import io.estatico.newtype.macros.newtype

object users {

  @derive(decoder, encoder, eqv, show)
  case class User(
      uuid: UserId,
      username: Username,
      password: Password,
      deleted: Deleted
  )

  case class UserCreate(username: Username, password: Password)

  case class UserUpdate(
      userId: UserId,
      username: Username,
      password: Password
  )

  case class UserDelete(userId: UserId)
}
