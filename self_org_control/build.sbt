name := """akka-scala-seed"""

version := "1.0"

scalaVersion := "2.10.4"

val sprayV = "1.3.3"

resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  // spray for http requests by the sensor
  "io.spray" %%  "spray-can"     % sprayV  withSources() withJavadoc(),
  "io.spray" %%  "spray-routing" % sprayV  withSources() withJavadoc(),
  "io.spray" %%  "spray-client"     % sprayV  withSources() withJavadoc(),
  // Change this to another test framework if you prefer
  "org.scalatest" %% "scalatest" % "2.1.6" % "test",
  // Akka
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3.5",
  "com.typesafe.akka" %% "akka-cluster" % "2.3.5",
  "ch.qos.logback" % "logback-classic" % "1.1.3",
  "com.fuzzylite" % "jfuzzylite" % "5.0",
  "org.jgroups" % "jgroups" % "3.6.9.Final",
  "com.watchtogether" % "wtRed5_common" % "0.0.1"
  //"com.typesafe.akka" %% "akka-testkit" % "2.3.5"
)