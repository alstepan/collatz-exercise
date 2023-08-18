package me.alstepan.collatz.core

import scala.concurrent.duration.*

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*
import cats.effect.std.*
import fs2.*
import fs2.concurrent.*

import me.alstepan.collatz.domain.*

class CollatzMachine[F[_]: Temporal] private(state: Ref[F, Event], stateChanges: Topic[F, Event], initial: Int, delay: FiniteDuration) {

  def collatz(n: Long): Long =       
    if (n % 2 == 0) n / 2 else 3 * n + 1

  def collatzStream: Stream[F, Event] =
    Stream
      .eval(state.get)
      .map { event => 
          event.copy(
            sequenceId = event.sequenceId + 1, 
            state = if (event.state == 1) initial else collatz(event.state)
          )      
      }
      .evalTap( event => state.update( e => event) )
      .evalTap( stateChanges.publish1 )         
      .metered(delay)
      .repeat

  def getStateChanges: Stream[F, Event] = 
      stateChanges.subscribe(1).repeat

  def increment(amount: Int): F[Unit] = 
    state.update(e => e.copy(state = e.state + amount))

}

object CollatzMachine {

  def apply[F[_]: Temporal](machineId: Int, initialValue: Int, delay: FiniteDuration): F[CollatzMachine[F]] =
    for {
      state <- Ref.of[F, Event](Event(machineId, 0, initialValue))
      buffer <- Topic[F, Event]
      _ <- buffer.publish1(Event(machineId, 0, initialValue))
      collatz = new CollatzMachine[F](state, buffer, initialValue, delay)
    } yield collatz
}