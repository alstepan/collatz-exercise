package me.alstepan.collatz.core

import scala.concurrent.duration.*

import cats.*
import cats.data.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import fs2.*

import me.alstepan.collatz.domain.*
import me.alstepan.collatz.core.*
import me.alstepan.collatz.config.*
import cats.effect.std.Supervisor

case class MachineRuntime[F[_]](machine: CollatzMachine[F], fiber: Fiber[F, Throwable, Unit])

class Machines[F[_]: Temporal] private (repo: Ref[F, Map[Int, MachineRuntime[F]]], config: CollatzConfig) {

  def start(id: Int, initialValue: Int): F[Either[Error, Unit]] =
    (for {
      machines <- EitherT.right(repo.get)
      // check if machine is already running
      exists <- EitherT.fromEither(machines.get(id).map(_ => MachineAlreadyExists(id).asInstanceOf[Error]).toLeft(()))
      // start new machine
      newMachine <- EitherT.right(CollatzMachine[F](id, initialValue, config.stateUpdateDelay))
      fiber <- EitherT.right(newMachine.collatzStream.compile.drain.start)
      // add machine to repository
      _<- EitherT.right(repo.update(m => m + (id ->  MachineRuntime(newMachine, fiber))))
    } yield ()).value

  def destroy(id: Int): F[Either[Error, Unit]] = 
    (for {
      machines <- EitherT.right(repo.get)
      runtime <- EitherT.fromEither[F](machines.get(id).toRight(MachineNotFound(id)))
      _ <- EitherT.right(runtime.fiber.cancel)
      _ <- EitherT.right(repo.update(m => m - id))
    } yield ()).value

  def events(id: Int): F[Either[Error, Stream[F, Event]]] =
    repo
      .get
      .map{ machines =>
        machines
          .get(id)
          .map(r => stateStream(r.machine))
          .toRight[Error](MachineNotFound(id))
      }
      
  def allEvents: Stream[F, Event] =
    Stream
      .eval(repo.get)
      .flatMap{ runtimes => 
        if (runtimes.isEmpty) Stream.evals(List().pure[F])
        else 
          runtimes
            .values
            .toList
            .map(r => stateStream(r.machine))          
            .reduce( _ merge _ )
      }

  def increment(id: Int, amount: Int): F[Either[Error, Unit]] =
    (for {
      machines <- EitherT.right(repo.get)
      runtime <- EitherT.fromEither[F](machines.get(id).toRight(MachineNotFound(id)))
      res <- EitherT.right(runtime.machine.increment(amount))
    } yield res).value

  def shutdown: F[Unit] = 
    for {
      runtimes <- repo.get
      a <- runtimes.values.toList.traverse(_.fiber.cancel)
    } yield ()

  private def stateStream(m: CollatzMachine[F]) = 
    m.getStateChanges  

}

object Machines {
  def apply[F[_]: Temporal](config: CollatzConfig): Resource[F, Machines[F]] =     
    for {
      repo <- Resource.eval(Ref.of[F, Map[Int, MachineRuntime[F]]](Map()))
      machines <- Resource.make(new Machines(repo, config).pure[F])(_.shutdown)
    } yield machines    
}
