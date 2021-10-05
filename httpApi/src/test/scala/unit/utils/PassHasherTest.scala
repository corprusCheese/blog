package unit.utils

import blog.domain.Password
import blog.utils.PassHasher
import weaver._
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Gen
import weaver.scalacheck.Checkers

object PassHasherTest extends SimpleIOSuite with Checkers {

  test("passwords encodes the same way every time") {
    val gen = Gen.chooseNum(1, 15).flatMap { n =>
      Gen.buildableOfN[String, Char](n, Gen.alphaChar)
    }.map(s => Password(NonEmptyString.unsafeFrom(s)))

    forall(gen) {
      password => {
        assert(PassHasher.hash(password) == PassHasher.hash(password))
      }
    }
  }
}
