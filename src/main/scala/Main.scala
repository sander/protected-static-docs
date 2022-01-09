package nl.sanderdijkhuis.docs

import cats.effect.{IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io._
import org.http4s.server.Router

import scala.concurrent.ExecutionContext.global

object Main extends IOApp.Simple:
  val port = 8080
  private val helloWorldService = HttpRoutes.of[IO] { case GET -> Root =>
    Ok(s"Hello, world!")
  }
  private val httpApp = Router("/" -> helloWorldService).orNotFound
  private val server = BlazeServerBuilder[IO](global)
    .bindHttp(port, "localhost")
    .withHttpApp(httpApp)
  val run = server.serve.compile.drain
