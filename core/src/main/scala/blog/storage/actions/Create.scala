package blog.storage.actions

trait Create[F[_], A] {
  def create(create: A): F[Unit]
}
