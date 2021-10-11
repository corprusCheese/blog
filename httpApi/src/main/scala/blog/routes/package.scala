package blog

import blog.auth.AuthCommands
import blog.domain.users.User
import blog.programs._
import blog.storage._
import cats.MonadThrow
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.JsonDecoder
import org.http4s.server.AuthMiddleware

package object routes {
  def getAll[F[_]: JsonDecoder: MonadThrow](
      us: UserStorageDsl[F],
      cs: CommentStorageDsl[F],
      ts: TagStorageDsl[F],
      ps: PostStorageDsl[F],
      am: AuthMiddleware[F, User],
      authCommands: AuthCommands[F]
  ): HttpRoutes[F] = {

    // programs

    val authProgram: AuthProgram[F] = AuthProgram.make(authCommands)
    val postProgram: PostProgram[F] = PostProgram.make(ps, cs, ts)
    val tagProgram: TagProgram[F] = TagProgram.make(ts, ps)
    val commentProgram: CommentProgram[F] = CommentProgram.make(cs, ps)

    // routes

    val authRoutes = Auth[F](authProgram).routes(am)
    val postRoutes = Posts[F](postProgram).routes(am)
    val commentRoutes = Comments[F](commentProgram).routes(am)
    val tagRoutes = Tags[F](tagProgram).routes(am)

    authRoutes <+> postRoutes <+> commentRoutes <+> tagRoutes
  }
}
