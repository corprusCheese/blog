package impl

import blog.domain._
import blog.domain.tags._
import blog.storage.TagStorageDsl
import cats.Monad
import cats.effect.{Ref, Resource}
import cats.implicits._
import eu.timepit.refined.auto._
import impl.helper.PostTagsStorage

case class TestTagStorage[F[_]: Monad](
    postTagsStorage: PostTagsStorage[F],
    inMemoryVector: Ref[F, Vector[Tag]]
) extends TagStorageDsl[F] {

  override def findById(id: TagId): F[Option[Tag]] =
    inMemoryVector.get.map(_.find(_.tagId == id))


  override def fetchAll: F[Vector[Tag]] =
    inMemoryVector.get

  override def findByName(name: TagName): F[Vector[Tag]] =
    inMemoryVector.get.map(_.filter(_.name == name))

  override def getAllPostTags(postId: PostId): F[Vector[Tag]] =
    postTagsStorage
      .findByPostId(postId)
      .flatMap(tagIds =>
        inMemoryVector.get.map(_.filter(inTag => tagIds.contains(inTag.tagId)))
      )

  override def delete(delete: TagDelete): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.filter(_.tagId != delete.tagId)
      _ <- inMemoryVector.set(newVector)
      posts <- postTagsStorage.findByTagId(delete.tagId)
      _ <- posts.map(postTagsStorage.deletePostId).sequence
    } yield ()

  override def create(create: TagCreate): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get :+ Tag(create.tagId, create.name)
      _ <- inMemoryVector.set(newVector)
      _ <- postTagsStorage.create(create.postsId, create.tagId)
    } yield ()

  override def update(update: TagUpdate): F[Unit] =
    for {
      get <- inMemoryVector.get
      newVector = get.map(tag =>
        if (tag.tagId == update.tagId)
          Tag(update.tagId, update.name)
        else tag
      )
      _ <- inMemoryVector.set(newVector)
      _ <- postTagsStorage.deleteTagId(update.tagId)
      _ <- postTagsStorage.create(update.postsId, update.tagId)
    } yield ()
}

object TestTagStorage {
  def resource[F[_]: Monad: Ref.Make]: Resource[F, TagStorageDsl[F]] =
    for {
      postTagStorage <- PostTagsStorage.resource[F]
      ref <- Resource.eval(
        Ref
          .of[F, Vector[Tag]](Vector.empty)
          .map(inMemoryVector =>
            TestTagStorage[F](postTagStorage, inMemoryVector)
          )
      )
    } yield ref
}
