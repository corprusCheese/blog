package blog.storage.actions

trait Delete [F[_], A]{
  def delete(delete: A): F[Unit]
}
