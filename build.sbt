import Dependencies._

lazy val root = (project in file("."))
  .settings(
    scalaVersion := "2.13.6",
    scalacOptions ~= (_.filterNot(Set("-Xfatal-warnings"))),
    libraryDependencies ++= Seq(
      L.http4s("ember-server"),
      L.http4s("ember-client"),
      L.http4s("circe"),
      L.http4s("dsl"),
      L.circe,
      L.logback,
      L.pureConfig,
      "com.softwaremill.sttp.client3" %% "http4s-backend" % "3.9.5",
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.5",
      "com.github.cb372" %% "cats-retry" % "3.1.0",
      T.munit,
      C.betterMonadicFor,
      C.kindProjector
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    libraryDependencySchemes ++= Seq(
      "org.http4s" %% "http4s-client" % "always"
    )
  )
