package me.alstepan.collatz.http

import scala.concurrent.duration.*

import io.circe.generic.auto.*
import io.circe.syntax.*
import org.http4s.circe.CirceEntityCodec.*

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.implicits.*

import fs2.*

import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.server.*

import org.typelevel.ci.CIStringSyntax

import me.alstepan.collatz.core.*
import me.alstepan.collatz.domain.*
import me.alstepan.collatz.config.*

class MachineRoutes[F[_]: Temporal] private(machines: Machines[F]) extends Http4sDsl[F] {
  val create:  HttpRoutes[F] = HttpRoutes.of {
    case POST -> root / "create" / IntVar(id) / IntVar(startingNumber) =>
      machines
        .start(id, startingNumber)
        .flatMap(handleEitherResponse(_, "Created"))
  }

  val destroy: HttpRoutes[F] = HttpRoutes.of {
    case POST -> root / "destroy" / IntVar(id) =>
      machines
        .destroy(id)
        .flatMap(handleEitherResponse(_, "Destroyed"))
  }

  val eventsByMachineId: HttpRoutes[F] = HttpRoutes.of {
    case GET -> root / "messages" / IntVar(id) =>
      machines
        .events(id)
        .flatMap{ outcome => 
          outcome.fold(
            error => handleError(error),
            events => handleStreamResponse(events.handleErrorWith(e => Stream.empty))
          )
        }
  }

  val allEvents: HttpRoutes[F] = HttpRoutes.of {
    case GET -> root / "messages" =>
      handleStreamResponse(machines.allEvents)
  }

  val incrementMachine: HttpRoutes[F] = HttpRoutes.of {
    case POST -> root / "increment" / IntVar(id) / IntVar(amount) =>
      machines
        .increment(id, amount)
        .flatMap(handleEitherResponse(_, s"Machine $id incremented by $amount"))
  }

  val routes = create <+> destroy <+> allEvents <+> eventsByMachineId <+> incrementMachine

  private def handleStreamResponse(messages: Stream[F, Event]) = 
    Ok(messages.map(m => ServerSentEvent(Some(m.asJson.spaces2))))
      .map(e => e.putHeaders(Header.Raw(ci"Content-Type", "text/event-stream")))

  private def handleEitherResponse(outcome: Either[Error, Unit], response: String) =
    outcome.fold(
      error => handleError(error),
      _ => Ok(s"""{"reponse": "$response"}""")
    )

  private def handleError(error: Error): F[Response[F]] = error match {
    case MachineNotFound(id)      => NotFound(s"""{"error": "Machine $id not found"}""")
    case MachineAlreadyExists(id) => Conflict(s"""{"error": "Machine $id already runnnig"}""")
  }
}

object MachineRoutes {
  def apply[F[_]: Temporal](config: CollatzConfig): Resource[F, MachineRoutes[F]] = 
    for {
      machines <- Machines(config)
    } yield new MachineRoutes(machines)
}