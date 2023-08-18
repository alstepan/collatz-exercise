package me.alstepan.collatz.core

import scala.concurrent.duration.*

import cats.* 
import cats.data.*
import cats.implicits.*

import cats.effect.*
import cats.effect.implicits.*

import org.scalatest.freespec.AsyncFreeSpec
import cats.effect.testing.scalatest.AsyncIOSpec
import me.alstepan.collatz.config.CollatzConfig
import org.scalatest.matchers.should.Matchers

class MachineSpec   
  extends AsyncFreeSpec
  with Matchers
  with AsyncIOSpec {

  val config = CollatzConfig(0.millis)

  "Machines algebra" - {
    "start" - {

      "Should return error if machine already exists" in {
        val program = Machines[IO](config).use { machines =>
          for {
            m1 <- machines.start(13, 123)
            m2 <- machines.start(13, 125)
          } yield m2
        }
        program.asserting(_.isLeft shouldBe true)
      }

      "Should start new machine if there is no machine with the same id runnning" in {
        val program = Machines[IO](config).use { machines =>
          for {
            m1 <- machines.start(13, 123)
            m2 <- machines.start(15, 125)
          } yield (m1, m2)
        }
        program.asserting{ case (m1, m2) => (m1.isRight, m2.isRight) shouldBe (true, true) }
      }      
    }

    "destroy" - {
      "Should return error for unknown ID" in {
        val program = Machines[IO](config).use { machines =>
          machines.destroy(123)
        }
        program.asserting(_.isLeft shouldBe true)
      }

      "Should sucessfully destroy machine with known id" in {
        val program = Machines[IO](config).use { machines =>
          for {
            _ <- machines.start(13, 123)
            _ <- machines.start(15, 125)
            d1 <- machines.destroy(13)
          } yield d1
        }
        program.asserting(_.isRight shouldBe true)
      }
    }

    "events" - {
      "Should return error for unknown ID" in {
        val program = Machines[IO](config).use { machines =>
          machines.events(123)
        }
        program.asserting(_.isLeft shouldBe true)
      }

      "Should return event stream for known ID" in {
        val program = Machines[IO](config).use { machines =>
          for {
            _ <- machines.start(13, 13)
            _ <- machines.start(15, 125)
            maybeEvents <- machines.events(13)
            events <- maybeEvents.map(_.take(5).compile.toList).traverse(identity)
          } yield events.getOrElse(List()).map(_.state)
        }
        program.asserting(_ shouldBe List(40, 20, 10, 5, 16))
      }      
    }

    "allEvents" - {
      "Should return empty stream when no machines created" in {
        val program = Machines[IO](config).use { machines =>
          for {
            events <- machines.allEvents.take(10).compile.toList           
          } yield events.map(_.state)
        }
        program.asserting(_ shouldBe List() )
      }

      "Should return combined sequence from multiple machines" in {
        val program = Machines[IO](config).use { machines =>
          for {
            _ <- machines.start(13, 13)
            _ <- machines.start(15, 125)
            events <- machines.allEvents.take(10).compile.toList           
          } yield events.map(_.state)
        }
        program.asserting(_ shouldBe List(20, 188, 10, 94, 47, 5, 16, 142, 8, 71) )        
      }
    }

    "increment" - {
      "Should return error for unknown ID" in {
        val program = Machines[IO](config).use { machines =>
          machines.increment(123, 456)
        }
        program.asserting(_.isLeft shouldBe true)
      }
      "Should increment stream by known value" in {
        val program = Machines[IO](config).use { machines =>
          for {
            _ <- machines.start(13, 13)
            _ <- machines.start(15, 125)
            _ <- machines.increment(13, 100)
            maybeEvents <- machines.events(13)
            events <- maybeEvents.map(_.take(5).compile.toList).traverse(identity)
          } yield events.getOrElse(List()).map(_.state)
        }
        program.asserting(_ shouldBe List(340, 170, 85, 256, 128))          
      }
    }
  }
}