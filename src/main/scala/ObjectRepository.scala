package nl.sanderdijkhuis.docs

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.data.EitherT
import cats.implicits._
import fs2.Stream
import software.amazon.awssdk.core.ResponseInputStream
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  GetObjectResponse,
  S3Exception
}

object ObjectRepository:

  val chunkSize = 4096

  case class GetResponse(properties: GetObjectResponse, body: Stream[IO, Byte])

  type GetObject = GetObjectRequest => IO[Either[S3Exception, GetResponse]]

  def resource(region: Region): Resource[IO, GetObject] = Resource {
    def response(r: ResponseInputStream[GetObjectResponse]) = GetResponse(
      r.response(),
      fs2.io.readInputStream(IO.pure(r), chunkSize, true)
    )
    def errorHandler(e: Throwable) = e match {
      case e: S3Exception => IO.pure(e)
      case e              => IO.raiseError(e)
    }
    for client <- IO(S3Client.builder().region(region).build())
    yield (
      (request) =>
        IO(client.getObject(request))
          .map(r => Right(response(r)))
          .handleErrorWith(e => errorHandler(e).map(Left.apply)),
      IO(client.close())
    )
  }
