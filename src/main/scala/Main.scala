package nl.sanderdijkhuis.docs

import fs2.Stream
import cats.data.EitherT
import cats.effect.{IO, IOApp}
import org.http4s.HttpRoutes
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io.*
import org.http4s.server.Router
import org.http4s.headers.`Content-Type`

import org.http4s.MediaType
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.global

object Main extends IOApp.Simple:
  val port = 8080
  private val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root => Ok(s"Hello, world!")
    case GET -> Root / "test" => {

      val request = GetObjectRequest
        .builder()
        .bucket("test-static-docs")
        .key("index.html")
        .build()

      val getObjectResource = ObjectRepository.resource(Region.EU_CENTRAL_1)
      val s = for
        get <- Stream.resource(getObjectResource)
        response <- Stream.eval(EitherT(get(request)).valueOrF(IO.raiseError))
        b <- response.body
      yield b

      Ok(s, `Content-Type`(MediaType.unsafeParse("text/html")))
    }
  }
  private val httpApp = Router("/" -> helloWorldService).orNotFound
  private val server = BlazeServerBuilder[IO](global)
    .bindHttp(port, "localhost")
    .withHttpApp(httpApp)
  val run = server.serve.compile.drain
