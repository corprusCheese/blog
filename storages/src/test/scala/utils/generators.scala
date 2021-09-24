package utils

import blog.domain._
import blog.domain.posts._
import blog.domain.users._
import eu.timepit.refined.types.string.NonEmptyString
import org.scalacheck.Gen

import java.util.UUID

object generators {

  val nonEmptyStringGen: Gen[String] =
    Gen
      .chooseNum(21, 40)
      .flatMap { n =>
        Gen.buildableOfN[String, Char](n, Gen.alphaChar)
      }

  def nesGen[A](f: String => A): Gen[A] =
    nonEmptyStringGen.map(f)

  def idGen[A](f: UUID => A): Gen[A] =
    Gen.uuid.map(f)

  val userIdGen: Gen[UserId] = idGen(UserId.apply)
  val userNameGen: Gen[Username] = nesGen(string => Username(NonEmptyString.unsafeFrom(string)))
  val userPasswordGen: Gen[Password] = nesGen(string => Password(NonEmptyString.unsafeFrom(string)))

  val userGen: Gen[User] =
    for {
      uuid <- userIdGen
      name <- userNameGen
      password <- userPasswordGen
    } yield User(uuid, name, password, false)


  val postIdGen: Gen[PostId] = idGen(PostId.apply)
  val postMessageGen: Gen[PostMessage] = nesGen(string => PostMessage(NonEmptyString.unsafeFrom(string)))

  val postGen: Gen[Post] =
    for {
      uuid <- postIdGen
      msg <- postMessageGen
      userId <- userIdGen
    } yield Post(uuid, msg, userId, false)
}
