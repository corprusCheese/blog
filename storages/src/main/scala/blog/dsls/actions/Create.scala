package blog.dsls.actions

trait Create[F[_], A] {
  def create(create: A): F[Unit]
}
