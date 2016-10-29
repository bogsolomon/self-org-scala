name := """akka-scala-seed"""

version := "1.0"

scalaVersion := "2.11.7"

val sprayV = "1.3.3"

libraryDependencies ++= Seq(
  // spray for http requests by the sensor
  "io.spray" %%  "spray-can"     % sprayV  withSources() withJavadoc(),
  "io.spray" %%  "spray-routing" % sprayV  withSources() withJavadoc(),
  "io.spray" %%  "spray-client"     % sprayV  withSources() withJavadoc(),
  // Change this to another test framework if you prefer
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.0" % "test",
  // Akka
  "com.typesafe.akka" %% "akka-actor" % "2.4.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.0",
  "ch.qos.logback" % "logback-classic" % "1.1.3"
)