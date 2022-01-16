package nl.sanderdijkhuis.docs

import fs2.Stream
import cats.data.EitherT
import cats.effect.{IO, IOApp}
import nl.sanderdijkhuis.docs.ObjectRepository.GetObject
import org.http4s.{HttpRoutes, MediaType, Response, Status}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.dsl.io.*
import org.http4s.*
import org.http4s.server.Router
import org.http4s.headers.`Content-Type`
import org.http4s.headers.Location
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import org.http4s.syntax.all._

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.global

object Main extends IOApp.Simple:
  val port = 8080
  val entryPoint = uri"/index.html"
  case object ContentTypeError extends Exception("could not parse content type")
  def reverseProxyService(getObject: GetObject) = HttpRoutes.of[IO] {
    case GET -> Root => Found().map(_.putHeaders(Location(entryPoint)))
    case GET -> path =>
      for
        response <- EitherT(
          getObject(
            GetObjectRequest
              .builder()
              .bucket("test-static-docs")
              .key(path.segments.mkString("/"))
              .build()
          )
        ).valueOrF(IO.raiseError)
        cty <- IO(
          `Content-Type`
            .parse(response.properties.contentType())
            .getOrElse(throw ContentTypeError)
        )
      yield Response(
        Status.Ok,
        body = response.body,
        headers = Headers(cty)
      )
  }
  private def httpApp(getObject: GetObject): HttpApp[IO] = Router(
    "/" -> reverseProxyService(getObject)
  ).orNotFound
  private def server(app: HttpApp[IO]) = BlazeServerBuilder[IO](global)
    .bindHttp(port, "localhost")
    .withHttpApp(app)
  val run = ObjectRepository.resource(Region.EU_CENTRAL_1).use { getObject =>
    server(httpApp(getObject)).serve.compile.drain
  }
