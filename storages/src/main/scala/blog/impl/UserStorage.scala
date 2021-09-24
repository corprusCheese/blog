package blog.impl

import blog.domain
import blog.domain.users
import blog.storage.UserStorageDsl
import cats.effect.Resource
import doobie.util.transactor

class UserStorage[F[_]] extends UserStorageDsl[F]{
  override def findById(id: domain.CommentId): F[Option[users.User]] = ???

  override def findByName(name: domain.Username): F[Vector[users.User]] = ???

  override def fetchAll: F[Vector[users.User]] = ???

  override def create(create: users.UserCreate): F[Unit] = ???

  override def update(update: users.UserUpdate): F[Unit] = ???

  override def delete(delete: users.UserDelete): F[Unit] = ???
}

object UserStorage {
  def make[F[_]](ta: transactor.Transactor[F]): Resource[F, UserStorageDsl[F]] = ???
}
