package impl

import blog.domain
import blog.domain._
import blog.domain.users._
import blog.impl.UserStorage
import blog.storage.UserStorageDsl
import cats.Monad
import cats.effect.{MonadCancelThrow, Resource}
import cats.implicits.catsSyntaxApplicativeId
import doobie.util.transactor.Transactor
import org.typelevel.log4cats.Logger

case class TestUserStorage[F[_]: Monad] () extends UserStorageDsl[F] {
  var inMemoryVector: Vector[User] = Vector.empty

  override def findById(id: UserId): F[Option[User]] = inMemoryVector.find(user => user.uuid == id).pure[F]

  override def findByName(name: Username): F[Option[User]] = inMemoryVector.find(user => user.username == name).pure[F]

  override def fetchAll: F[Vector[User]] = inMemoryVector.pure[F]

  override def delete(delete: UserDelete): F[Unit] = {
    inMemoryVector = inMemoryVector.filter(user => user.uuid != delete.userId)
    Monad[F].unit
  }

  override def create(create: UserCreate): F[Unit] = {
    inMemoryVector = inMemoryVector :+ User(create.userId, create.username, create.password)
    Monad[F].unit
  }

  override def update(update: UserUpdate): F[Unit] = {
    Monad[F].unit
  }
}

object TestUserStorage {
  def resource[F[_]: Logger: MonadCancelThrow]: Resource[F, UserStorageDsl[F]] =
    Resource.pure[F, UserStorageDsl[F]](TestUserStorage[F]())
}
