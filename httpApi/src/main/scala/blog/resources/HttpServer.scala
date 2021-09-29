package blog.resources

import cats.effect.kernel.{Async, Resource}
import com.comcast.ip4s._
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger


trait HttpServer[F[_]] {
  def newEmber(httpApp: HttpApp[F]): Resource[F, Server]
}

object HttpServer {
  def apply[F[_]: HttpServer]: HttpServer[F] = implicitly

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  implicit def forAsyncLogger[F[_]: Async: Logger]: HttpServer[F] =
    (httpApp: HttpApp[F]) => EmberServerBuilder
      .default[F]
      .withPort(Port.fromInt(8080).get)
      .withHttpApp(httpApp)
      .build
      .evalTap(showEmberBanner[F])
}
