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
  "ch.qos.logback" % "logback-classic" % "1.2.10",
  "software.amazon.awssdk" % "bom" % "2.17.107" pomOnly (),
  "software.amazon.awssdk" % "s3" % "2.17.107",
  // "com.amazonaws" % "aws-java-sdk" % "1.12.136",
  "org.typelevel" %% "munit-cats-effect-3" % "1.0.6" % Test
)
