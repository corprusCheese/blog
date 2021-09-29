package blog.utils

import blog.domain._
import eu.timepit.refined.types.all.NonEmptyString

import java.security.MessageDigest

object PassHasher {

  private def md5(s: String): Array[Byte] =
    MessageDigest.getInstance("MD5").digest(s.getBytes)

  private def hex(bytes: Array[Byte]): HashedPass =
    NonEmptyString.unsafeFrom(BigInt(1, bytes).toString(16).toUpperCase)

  def hash(pass: Password): HashedPassword =
    HashedPassword(hex(md5(pass.value.value)))

}
