package ext

import derevo.circe.magnolia._
import derevo.derive
import eu.timepit.refined.cats._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.refined._

@derive(decoder, encoder)
case class RefinedClass(nes1: NonEmptyString)
