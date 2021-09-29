package blog

import blog.domain._
import cats.Show
import cats.syntax.all._
import doobie._
import io.circe.Json
import io.circe.jawn._
import org.postgresql.util.PGobject
import doobie.postgres._
import doobie.postgres.implicits._
import eu.timepit.refined.types.string.NonEmptyString
import doobie.refined.implicits.refinedMeta

import java.util.UUID

object meta {
  implicit val showPGobject: Show[PGobject] = Show.show(_.getValue.take(250))

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced
      .other[PGobject]("json")
      .timap[Json](a => parse(a.getValue).leftMap[Json](e => throw e).merge)(
        a => {
          val o = new PGobject
          o.setType("json")
          o.setValue(a.noSpaces)
          o
        }
      )

  // ids

  implicit val userIdMeta: Meta[UserId] =
    Meta[UUID].timap(UserId.apply)(_.value)
  implicit val tagIdMeta: Meta[TagId] = Meta[UUID].timap(TagId.apply)(_.value)
  implicit val postIdMeta: Meta[PostId] =
    Meta[UUID].timap(PostId.apply)(_.value)
  implicit val commentIdMeta: Meta[CommentId] =
    Meta[UUID].timap(CommentId.apply)(_.value)

  // nes

  implicit val nesMeta: Meta[NonEmptyString] = refinedMeta

  implicit val usernameMeta: Meta[Username] =
    Meta[NonEmptyString].timap(Username.apply)(_.value)
  implicit val postMessageMeta: Meta[PostMessage] =
    Meta[NonEmptyString].timap(PostMessage.apply)(_.value)
  implicit val commentMessageMeta: Meta[CommentMessage] =
    Meta[NonEmptyString].timap(CommentMessage.apply)(_.value)
  implicit val tagNameMeta: Meta[TagName] =
    Meta[NonEmptyString].timap(TagName.apply)(_.value)

  // password

  implicit val passwordMeta: Meta[Password] =
    Meta[NonEmptyString].timap(Password.apply)(_.value)
  implicit val hashedPasswordMeta: Meta[HashedPassword] =
    Meta[NonEmptyString].timap(HashedPassword.apply)(_.value)

  // path

  private def ltree2PathForDb(str: String): String = str.replace("-", "s")
  private def pathForDb2ltree(str: String): String = str.replace("s", "-")

  implicit val pathMeta: Meta[CommentMaterializedPath] =
    Meta[String].timap(x => CommentMaterializedPath.apply(pathForDb2ltree(x)))(
      x => ltree2PathForDb(x.value)
    )

}
