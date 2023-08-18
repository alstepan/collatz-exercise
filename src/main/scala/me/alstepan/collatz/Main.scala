package me.alstepan.collatz

import scala.concurrent.duration.*
import cats.effect.*
import org.http4s.ember.server.EmberServerBuilder

import pureconfig.*

import me.alstepan.collatz.http.MachineRoutes
import me.alstepan.collatz.config.*
import me.alstepan.collatz.config.syntax.* 
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.Logger

object Main extends IOApp {
  
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  override def run(args: List[String]): IO[ExitCode] =
    ConfigSource.default.loadF[IO, AppConfig].flatMap { config => 
      val resources = for {
        machineApi <- MachineRoutes[IO](config.collatzConfig)
        server <- EmberServerBuilder
          .default[IO]
          .withHost(config.emberConfig.host)
          .withPort(config.emberConfig.port)
          .withHttpApp(machineApi.routes.orNotFound)
          .build
      } yield server

      resources.use(_ => IO.never).as(ExitCode.Success)
    }

}
