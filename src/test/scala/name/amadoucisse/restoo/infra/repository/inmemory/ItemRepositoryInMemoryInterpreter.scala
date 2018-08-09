package name.amadoucisse.restoo
package infra
package repository.inmemory

import java.util.Random

import cats._
import cats.implicits._
import domain.items._
import domain.AppError

import scala.collection.concurrent.TrieMap

final class ItemRepositoryInMemoryInterpreter[F[_]: Monad] extends ItemRepositoryAlgebra[F] {

  private val cache = new TrieMap[ItemId, Item]
  private val random = new Random

  def create(item: Item): F[AppError Either Item] = findByName(item.name).map {
    case Some(_) => AppError.itemAlreadyExists(item).asLeft
    case None =>
      val id = ItemId(random.nextInt.abs)
      val toSave = item.copy(id = id.some)
      cache += (id -> toSave)
      toSave.asRight

  }

  def update(item: Item): F[Option[Item]] =
    item.id
      .map { id =>
        cache.update(id, item)
        item
      }
      .pure[F]

  def get(id: ItemId): F[Option[Item]] =
    cache.get(id).pure[F]

  def findByName(name: Name): F[Option[Item]] =
    cache.values.find(u => u.name == name).pure[F]

  def delete(itemId: ItemId): F[Unit] = {
    cache.remove(itemId)
    ().pure[F]
  }

  def list(category: Option[Category]): fs2.Stream[F, Item] = fs2.Stream.emits {
    category match {
      case Some(c) => cache.values.filter(_.category == c).toVector.sortBy(_.name.value)
      case None => cache.values.toVector.sortBy(_.name.value)
    }
  }
}

object ItemRepositoryInMemoryInterpreter {
  def apply[F[_]: Monad]: ItemRepositoryInMemoryInterpreter[F] =
    new ItemRepositoryInMemoryInterpreter[F]
}
