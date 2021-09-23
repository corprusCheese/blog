package blog

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import io.estatico.newtype.macros.newtype

import java.util.UUID

package object domain {

  @derive(encoder, decoder)
  @newtype
  case class UserId(value: UUID)

  @derive(encoder, decoder)
  @newtype
  case class Username(value: String)
}
