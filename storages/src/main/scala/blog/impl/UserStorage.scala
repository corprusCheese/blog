package blog.impl

import blog.domain._
import blog.domain.users._
import blog.meta._
import blog.storage.UserStorageDsl
import cats.effect._
import cats.syntax.all._
import doobie.implicits._
import doobie.util.transactor._
import org.typelevel.log4cats._

case class UserStorage[F[_]: Logger: MonadCancelThrow](tx: Transactor[F])
    extends UserStorageDsl[F] {

  override def findById(id: UserId): F[Option[User]] =
    sql"""SELECT uuid, name, password, deleted FROM users WHERE uuid = ${id}"""
      .query[(UserId, Username, HashedPassword, Deleted)]
      .option
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding user by id = ${id}"))
      .map(_.map {
        case (userId, username, password, deleted) =>
          User(userId, username, password, deleted)
      })

  override def findByName(name: Username): F[Option[User]] =
    sql"""SELECT uuid, name, password, deleted FROM users WHERE name = ${name}"""
      .query[(UserId, Username, HashedPassword, Deleted)]
      .option
      .transact(tx)
      .flatTap(_ => Logger[F].info(s"finding by name ${name}"))
      .map(_.map {
        case (userId, username, password, deleted) =>
          User(userId, username, password, deleted)
      })

  override def fetchAll: F[Vector[User]] =
    sql"""SELECT uuid, name, password, deleted FROM users WHERE deleted = false"""
      .query[(UserId, Username, HashedPassword, Deleted)]
      .to[Vector]
      .transact(tx)
      .flatTap(_ => Logger[F].info("fetching all users"))
      .map(_.map {
        case (userId, username, password, deleted) =>
          User(userId, username, password, deleted)
      })

  override def create(create: UserCreate): F[Unit] =
    sql"""INSERT INTO users (uuid, name, password) VALUES (${create.userId}, ${create.username}, ${create.password})""".update.run
      .transact(tx)
      .flatTap(_ => Logger[F].info("creating new user"))
      .map(_ => ())

  override def update(update: UserUpdate): F[Unit] =
    sql"""UPDATE users SET name = ${update.username}, password = MD5(${update.password}) WHERE uuid = ${update.userId}""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"updating user with id = ${update.userId}")
      )
      .map(_ => ())

  override def delete(delete: UserDelete): F[Unit] =
    sql"""UPDATE users SET deleted = true WHERE uuid = ${delete.userId}""".update.run
      .transact(tx)
      .flatTap(_ =>
        Logger[F].info(s"deleting user with id = ${delete.userId}")
      )
      .map(_ => ())
}

object UserStorage {
  def resource[F[_]: Logger: MonadCancelThrow](
      tx: Transactor[F]
  ): Resource[F, UserStorageDsl[F]] =
    Resource.pure[F, UserStorage[F]](UserStorage[F](tx))
}
