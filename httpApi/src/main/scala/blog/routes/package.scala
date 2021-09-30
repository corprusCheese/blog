package blog

import blog.domain.users.User
import blog.storage._
import cats.MonadThrow
import dev.profunktor.redis4cats.RedisCommands
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.AuthMiddleware
import cats._
import cats.data._
import cats.implicits._

package object routes {
  def getAll[F[_]: JsonDecoder: MonadThrow](
      ac: AuthCommandsDsl[F],
      us: UserStorageDsl[F],
      cs: CommentStorageDsl[F],
      ts: TagStorageDsl[F],
      ps: PostStorageDsl[F],
      am: AuthMiddleware[F, User]
  ): HttpRoutes[F] = {
    val authRoutes = Auth[F](ac).routes(am)
    val postRoutes = Posts[F](ps, cs, ts).routes(am)

    authRoutes <+> postRoutes
  }
}
