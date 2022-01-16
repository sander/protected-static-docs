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
import org.http4s.headers.*
import org.http4s.headers.Location
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import org.http4s.syntax.all._

import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.global

object Main extends IOApp.Simple:
  val port = 8080
  val entryPoint = uri"/index.html"
  case object HeaderParserError extends Exception("could not parse header")
  private def header[T](result: ParseResult[T]): IO[T] = IO(
    result.getOrElse(throw HeaderParserError)
  )
  def reverseProxyService(getObject: GetObject) = HttpRoutes.of[IO] {
    case GET -> Root => Found().map(_.putHeaders(Location(entryPoint)))
    case request @ GET -> path =>
      for
        r <- EitherT(
          getObject(
            GetObjectRequest
              .builder()
              .bucket("test-static-docs")
              .key(path.segments.mkString("/"))
              .ifNoneMatch(request.headers.get[`If-None-Match`] match {
                case Some(h) => h.toString
                case None    => null
              })
              .build()
          )
        ).valueOrF(IO.raiseError)
        cty <- header(`Content-Type`.parse(r.properties.contentType()))
        cl <- header(`Content-Length`.fromLong(r.properties.contentLength()))
        etag <- header(ETag.parse(r.properties.eTag()))
      yield Response(
        Status.Ok,
        body = r.body,
        headers = Headers(cty, cl, etag)
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
