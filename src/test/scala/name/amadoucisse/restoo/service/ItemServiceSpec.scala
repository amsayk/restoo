package name.amadoucisse.restoo
package service

import cats.Applicative
import cats.implicits._
import cats.effect.IO
import domain.DateTime
import repository.ItemRepository
import domain.items._
import domain.AppError
import common.IOAssertion
import http.{ Page, SortBy }
import org.scalatest.{ MustMatchers, WordSpec }
import eu.timepit.refined.auto._

import service.Items._

import cats.mtl.{ ApplicativeAsk, DefaultApplicativeAsk }

import java.time.Instant

class ItemServiceSpec extends WordSpec with MustMatchers with IOExecution {
  val newItemId = ItemId(1)

  val expectedName = Name("Item name")
  val expectedPrice = Money(999, "MAD")
  val expectedCategory = Category("Category")

  val existingName = Name("Item existing name")

  val now = DateTime(Instant.now)

  "add item" when {

    "name is unique" should {
      "create item" in new Context {

        val item = Item(
          name = expectedName,
          price = expectedPrice,
          category = expectedCategory,
          createdAt = now,
          updatedAt = now,
          id = newItemId,
        )

        val op = createItem[IO](item)

        IOAssertion {

          for {
            _ ← op
            _ = item.id mustBe newItemId
          } yield ()
        }

      }
    }

    "name is not unique" should {
      "fail" in new Context {

        val item = Item(
          name = existingName,
          price = expectedPrice,
          category = expectedCategory,
          createdAt = now,
          updatedAt = now,
          id = newItemId,
        )

        val op = createItem[IO](item)

        a[AppError.ItemAlreadyExists] should be thrownBy IOAssertion {
          op
        }

      }
    }

  }

  "update item" when {

    "name is unique" should {
      "update item" in new Context {

        val item = Item(
          name = expectedName,
          price = expectedPrice,
          category = expectedCategory,
          createdAt = now,
          updatedAt = now,
          id = newItemId,
        )

        val op = updateItem[IO](item)

        IOAssertion {

          for {
            _ ← op
            _ = item.id mustBe newItemId
          } yield ()
        }

      }
    }

    "name is not unique" should {
      "fail" in new Context {

        val item = Item(
          name = existingName,
          price = expectedPrice,
          category = expectedCategory,
          createdAt = now,
          updatedAt = now,
          id = newItemId,
        )

        val op = updateItem[IO](item)

        a[AppError.ItemAlreadyExists] should be thrownBy IOAssertion {
          op
        }

      }
    }

  }

  final class ItemRepositoryImpl extends ItemRepository[IO] {
    def create(item: Item): IO[Unit] = (item.name, item.price, item.category) match {
      case (`expectedName`, `expectedPrice`, `expectedCategory`) ⇒
        ().pure[IO]

      case (`existingName`, _, _) ⇒ IO.raiseError(AppError.itemAlreadyExists(item))

      case _ ⇒ IO.raiseError(new RuntimeException("Unexpected parameter"))
    }

    def update(item: Item): IO[Unit] = (item.name, item.price, item.category) match {
      case (`expectedName`, `expectedPrice`, `expectedCategory`) ⇒
        ().pure[IO]

      case (`existingName`, _, _) ⇒ IO.raiseError(AppError.itemAlreadyExists(item))

      case _ ⇒ IO.raiseError(new RuntimeException("Unexpected parameter"))
    }

    def get(id: ItemId): IO[Item] =
      Item(
        name = expectedName,
        price = expectedPrice,
        category = expectedCategory,
        createdAt = now,
        updatedAt = now,
        id = newItemId,
      ).pure[IO]

    def findByName(name: Name): IO[Item] = ???

    def delete(itemId: ItemId): IO[Unit] = ???

    def list(category: Option[Category], orderBy: Seq[SortBy], page: Page): IO[Vector[Item]] = ???
  }

  trait Context {
    val itemRepo = new ItemRepositoryImpl

    implicit val itemsInstance: ApplicativeAsk[IO, Items[IO]] = new DefaultApplicativeAsk[IO, Items[IO]] {
      val applicative: Applicative[IO] = Applicative[IO]
      def ask: IO[Items[IO]] =
        IO.pure(new Items[IO] {
          def items: Items.Service[IO] = Items.Live[IO](itemRepo)
        })
    }

  }
}
