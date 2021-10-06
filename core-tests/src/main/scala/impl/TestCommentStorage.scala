package impl

import blog.domain._
import blog.domain.comments._
import blog.storage.CommentStorageDsl
import cats.Monad
import cats.effect._
import cats.implicits._

case class TestCommentStorage[F[_]: Monad](
    inMemoryVector: Ref[F, Vector[CustomComment]]
) extends CommentStorageDsl[F] {
  override def findById(id: CommentId): F[Option[CustomComment]] =
    inMemoryVector.get.map(_.find(_.commentId == id))

  override def fetchAll: F[Vector[CustomComment]] = inMemoryVector.get

  override def getActiveUserComments(userId: UserId): F[Vector[CustomComment]] =
    inMemoryVector.get.map(_.filter(_.userId == userId))

  override def getAllPostComments(postId: PostId): F[Vector[CustomComment]] =
    inMemoryVector.get.map(
      _.filter(comment => comment.path.value.contains(postId.show))
    )

  override def deleteAllPostComments(postId: PostId): F[Unit] =
    for {
      postComments <- getAllPostComments(postId)
      get <- inMemoryVector.get
      newVector = get.filter(comment => !postComments.contains(comment))
      _ <- inMemoryVector.set(newVector)
    } yield ()

  override def delete(delete: DeleteComment): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.filter(_.commentId != delete.commentId)
      _ <- inMemoryVector.set(newVector)
    } yield ()

  override def create(create: CreateComment): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get :+ Comment(
        create.commentId,
        create.message,
        create.userId,
        create.path
      )
      _ <- inMemoryVector.set(newVector)
    } yield ()

  override def update(update: UpdateComment): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.map(comment =>
        if (comment.commentId == update.commentId)
          Comment(
            update.commentId,
            update.message,
            comment.userId,
            comment.path
          )
        else comment
      )
      _ <- inMemoryVector.set(newVector)
    } yield ()
}

object TestCommentStorage {
  def resource[F[_]: Monad: Ref.Make]: Resource[F, CommentStorageDsl[F]] =
    Resource.eval(
      Ref.of[F, Vector[CustomComment]](Vector.empty).map(TestCommentStorage[F])
    )
}
