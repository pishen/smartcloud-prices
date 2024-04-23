import sbt._

object Dependencies {

  object V { // Versions
    // Scala

    val http4s     = "1.0.0-M29"
    val circe      = "0.15.0-M1"
    val logback    = "1.2.6"
    val pureConfig = "0.16.0"

    // Test
    val munit = "0.7.29"

    // Compiler
    val betterMonadicFor = "0.3.1"
    val kindProjector    = "0.13.2"
  }

  object L { // Libraries
    // Scala
    def http4s(module: String): ModuleID = "org.http4s" %% s"http4s-$module" % V.http4s

    val circe      = "io.circe"              %% "circe-generic"   % V.circe
    val logback    = "ch.qos.logback"         % "logback-classic" % V.logback
    val pureConfig = "com.github.pureconfig" %% "pureconfig"      % V.pureConfig
  }

  object T { // Test dependencies
    // Scala
    val munit = "org.scalameta" %% "munit" % V.munit % Test
  }

  object C { // Compiler plugins
    val betterMonadicFor = compilerPlugin("com.olegpy" %% "better-monadic-for" % V.betterMonadicFor)
    val kindProjector    = compilerPlugin("org.typelevel" %% "kind-projector" % V.kindProjector cross CrossVersion.full)
  }

}
