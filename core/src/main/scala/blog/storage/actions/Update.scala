package blog.storage.actions

trait Update[F[_], A] {
  def update(update: A): F[Unit]
}
