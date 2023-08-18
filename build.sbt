ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val scala3Version = "3.3.0"

lazy val circeVersion               = "0.14.5"
lazy val catsEffectVersion          = "3.5.0"
lazy val http4sVersion              = "0.23.19"
lazy val pureConfigVersion          = "0.17.2"
lazy val scalaTestVersion           = "3.2.15"
lazy val scalaTestCatsEffectVersion = "1.4.0"
lazy val logbackVersion             = "1.4.7"
lazy val slf4jVersion               = "2.0.5"

lazy val server = (project in file("."))
  .enablePlugins(  
    JavaAppPackaging,
    DockerPlugin,
    AshScriptPlugin
  )
  .settings(
    name         := "CollatzMachine",
    scalaVersion := scala3Version,
    organization := "me.alstepan",
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"         % catsEffectVersion,
      "org.http4s"            %% "http4s-dsl"          % http4sVersion,
      "org.http4s"            %% "http4s-ember-server" % http4sVersion,
      "org.http4s"            %% "http4s-circe"        % http4sVersion,
      "io.circe"              %% "circe-generic"       % circeVersion,
      "com.github.pureconfig" %% "pureconfig-core"     % pureConfigVersion,
      "org.slf4j"              % "slf4j-simple"        % slf4jVersion,
      "org.scalatest"         %% "scalatest"                     % scalaTestVersion % Test,
      "org.typelevel"         %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "ch.qos.logback"         % "logback-classic"               % logbackVersion             % Test
    ),
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "amazoncorretto:17-alpine3.18",
    Compile / mainClass := Some("me.alstepan.collatz.Main")
  )
