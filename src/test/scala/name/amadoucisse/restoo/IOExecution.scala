package name.amadoucisse.restoo

import cats.effect.{ ContextShift, IO, Timer }
import cats.Parallel

import scala.concurrent.ExecutionContext

trait IOExecution {

  implicit lazy val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  implicit lazy val timer: Timer[IO] = IO.timer(ExecutionContext.global)

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  implicit lazy val par: Parallel[IO, IO] = Parallel[IO, IO.Par].asInstanceOf[Parallel[IO, IO]]
}
