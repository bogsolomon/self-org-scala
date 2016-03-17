name := """akka-scala-seed"""

version := "1.0"

scalaVersion := "2.10.4"

val sprayV = "1.3.3"

libraryDependencies ++= Seq(
  // spray for http requests by the sensor
  "io.spray" %%  "spray-can"     % sprayV  withSources() withJavadoc(),
  "io.spray" %%  "spray-routing" % sprayV  withSources() withJavadoc(),
  "io.spray" %%  "spray-client"     % sprayV  withSources() withJavadoc(),
  // Change this to another test framework if you prefer
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  // Akka
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  //"com.typesafe.akka" %% "akka-remote" % "2.3.5",
  //"com.typesafe.akka" %% "akka-testkit" % "2.3.5"
)
