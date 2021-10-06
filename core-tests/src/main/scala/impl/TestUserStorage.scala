package impl

import blog.domain._
import blog.domain.users._
import blog.storage.UserStorageDsl
import cats.Monad
import cats.effect.{Ref, Resource}
import cats.implicits._

case class TestUserStorage[F[_]: Monad](inMemoryVector: Ref[F, Vector[User]])
    extends UserStorageDsl[F] {

  override def findById(id: UserId): F[Option[User]] =
    inMemoryVector.get.map(_.find(_.uuid == id))

  override def findByName(name: Username): F[Option[User]] =
    inMemoryVector.get.map(_.find(_.username == name))

  override def fetchAll: F[Vector[User]] = inMemoryVector.get

  override def delete(delete: UserDelete): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.filter(_.uuid != delete.userId)
      _ <- inMemoryVector.set(newVector)
    } yield ()

  override def create(create: UserCreate): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get :+ User(create.userId, create.username, create.password)
      _ <- inMemoryVector.set(newVector)
    } yield ()

  override def update(update: UserUpdate): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.map(user =>
        if (user.uuid == update.userId)
          User(update.userId, update.username, update.password)
        else user
      )
      _ <- inMemoryVector.set(newVector)
    } yield ()

}

object TestUserStorage {
  def resource[F[_]: Monad: Ref.Make]: Resource[F, UserStorageDsl[F]] =
    Resource.eval(
      Ref.of[F, Vector[User]](Vector.empty).map(TestUserStorage[F])
    )
}
