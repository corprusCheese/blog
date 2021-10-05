package blog.resources

import blog.config.HttpServerConfig
import cats.effect.kernel.{Async, Resource}
import com.comcast.ip4s.IpLiteralSyntax
import org.http4s.HttpApp
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.defaults.Banner
import org.typelevel.log4cats.Logger


trait HttpServer[F[_]] {
  def newEmber(httpApp: HttpApp[F], http4sServerConfig: HttpServerConfig): Resource[F, Server]
}

object HttpServer {
  def apply[F[_]: HttpServer]: HttpServer[F] = implicitly

  private def showEmberBanner[F[_]: Logger](s: Server): F[Unit] =
    Logger[F].info(s"\n${Banner.mkString("\n")}\nHTTP Server started at ${s.address}")

  implicit def forAsyncLogger[F[_]: Async: Logger]: HttpServer[F] =
    (httpApp: HttpApp[F], config: HttpServerConfig) => EmberServerBuilder
      .default[F]
      .withPort(config.port)
      .withHost(config.host)
      .withHttpApp(httpApp)
      .build
      .evalTap(showEmberBanner[F])
}
