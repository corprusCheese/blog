package impl

import blog.domain._
import blog.storage.AuthCacheDsl
import cats.Monad
import cats.effect.Resource
import cats.effect.kernel.Ref
import dev.profunktor.auth.jwt._
import cats.implicits._

import scala.concurrent.duration._

case class TestAuthCache[F[_]: Monad](
    inMemoryVector: Ref[F, Vector[(String, String)]]
) extends AuthCacheDsl[F] {
  override def getTokenAsString(userId: UserId): F[Option[String]] =
    inMemoryVector.get.map(_.find(_._1 == userId.show).map(_._2))

  override def getUserAsString(token: JwtToken): F[Option[String]] =
    inMemoryVector.get.map(_.find(_._2 == token.value).map(_._1))

  override def setToken(
      user: users.User,
      token: JwtToken,
      timeout: FiniteDuration = 10.seconds
  ): F[Unit] =
    inMemoryVector.update(_ :+ (user.userId.show, token.value))

  override def delToken(userId: UserId, token: JwtToken): F[Unit] =
    inMemoryVector.update(
      _.filter(c => c._1 != userId.show && c._2 != token.value)
    )
}

object TestAuthCache {
  def resource[F[_]: Monad: Ref.Make]: Resource[F, AuthCacheDsl[F]] =
    Resource.eval(
      Ref
        .of[F, Vector[(String, String)]](Vector.empty)
        .map(inMemoryVector => TestAuthCache[F](inMemoryVector))
    )
}
