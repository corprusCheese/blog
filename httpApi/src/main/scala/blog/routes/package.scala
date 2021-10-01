package blog

import blog.domain.users.User
import blog.storage._
import cats.MonadThrow
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.AuthMiddleware

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
    val commentRoutes = Comments[F](cs, ps).routes(am)
    val tagRoutes = Tags[F](ts, ps).routes(am)

    authRoutes <+> postRoutes <+> commentRoutes <+> tagRoutes
  }
}