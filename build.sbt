name := "protected-static-docs"

version := "0.1"

scalaVersion := "3.1.0"

idePackagePrefix := Some("nl.sanderdijkhuis.docs")

val http4sVersion = "0.23.7"

libraryDependencies ++= Seq(
  "org.http4s" % "http4s-core_3" % http4sVersion,
  "org.http4s" % "http4s-dsl_3" % http4sVersion,
  "org.http4s" % "http4s-blaze-server_3" % http4sVersion,
  "org.http4s" % "http4s-blaze-client_3" % http4sVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.10"
)
