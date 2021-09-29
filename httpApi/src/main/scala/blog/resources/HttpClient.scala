package blog.resources

import cats.effect.{Async, Resource}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait HttpClient[F[_]] {
  def newEmber: Resource[F, Client[F]]
}

object HttpClient {
  def apply[F[_]: HttpClient]: HttpClient[F] = implicitly

  implicit def forAsync[F[_]: Async]: HttpClient[F] =
    new HttpClient[F] {
      def newEmber: Resource[F, Client[F]] =
        EmberClientBuilder
          .default[F]
          .build
    }
}