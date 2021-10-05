package utils

import blog.domain._
import blog.domain.posts._
import blog.domain.users._
import blog.domain.tags._
import blog.domain.comments._
import dev.profunktor.auth.jwt.JwtToken
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
  val userNameGen: Gen[Username] =
    nesGen(string => Username(NonEmptyString.unsafeFrom(string)))
  val userPasswordGen: Gen[HashedPassword] =
    nesGen(string => HashedPassword(NonEmptyString.unsafeFrom(string)))

  val userGen: Gen[User] =
    for {
      uuid <- userIdGen
      name <- userNameGen
      password <- userPasswordGen
    } yield User(uuid, name, password)

  val postIdGen: Gen[PostId] = idGen(PostId.apply)
  val postMessageGen: Gen[PostMessage] =
    nesGen(string => PostMessage(NonEmptyString.unsafeFrom(string)))

  val postGen: Gen[Post] =
    for {
      uuid <- postIdGen
      msg <- postMessageGen
      userId <- userIdGen
    } yield Post(uuid, msg, userId)

  val tagIdGen: Gen[TagId] = idGen(TagId.apply)
  val tagNameGen: Gen[TagName] =
    nesGen(string => TagName(NonEmptyString.unsafeFrom(string)))

  val tagGen: Gen[Tag] =
    for {
      uuid <- tagIdGen
      name <- tagNameGen
    } yield Tag(uuid, name)

  val commentIdGen: Gen[CommentId] = idGen(CommentId.apply)
  val commentMessageGen: Gen[CommentMessage] =
    nesGen(string => CommentMessage(NonEmptyString.unsafeFrom(string)))

  val commentGen: Gen[Comment] =
    for {
      uuid <- commentIdGen
      name <- commentMessageGen
      userId <- userIdGen
    } yield Comment(uuid, name, userId, CommentMaterializedPath(""))

  val tokenAsStringGen: Gen[String] = nesGen(s => s)

}
