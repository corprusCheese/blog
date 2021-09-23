package blog.dsls.actions

trait Delete [F[_], A]{
  def delete(delete: A): F[Unit]
}
