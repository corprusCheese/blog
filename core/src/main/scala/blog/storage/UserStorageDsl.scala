package blog.storage

import blog.domain._
import blog.domain.users._
import blog.storage.combined.CreateUpdateDelete

trait UserStorageDsl[F[_]]
    extends CreateUpdateDelete[F, UserCreate, UserUpdate, UserDelete] {

  def findById(id: UserId): F[Option[User]]
  def findByName(name: Username): F[Vector[User]]
  def fetchAll: F[Vector[User]]
}
