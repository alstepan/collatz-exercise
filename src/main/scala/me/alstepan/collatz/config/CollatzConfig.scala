package me.alstepan.collatz.config

import scala.concurrent.duration.*

import cats.implicits.*

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*
import pureconfig.error.*
import pureconfig.error.FailureReason

final case class CollatzConfig(stateUpdateDelay: FiniteDuration) derives ConfigReader

object CollatzConfig {
  given hostReader: ConfigReader[FiniteDuration] = ConfigReader[String].emap { durationString =>
    Either
      .catchNonFatal(Duration(durationString))
      .leftMap(_ => CannotConvert(durationString, Duration.getClass.toString, s"Invalid duration $durationString"))
      .flatMap { d => 
        Either
          .fromOption(
            Option(d)
              .collect{ case d: FiniteDuration => d}, 
            CannotConvert(durationString, Duration.getClass.toString, s"Finite duration is required: $durationString")
          )
      }
  }
}
