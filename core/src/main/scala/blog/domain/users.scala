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
      password: HashedPassword,
      deleted: Deleted = false
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
