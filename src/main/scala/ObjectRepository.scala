package nl.sanderdijkhuis.docs

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.data.EitherT
import cats.implicits.*
import fs2.Stream
import org.http4s.Status.NotModified
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

  private val chunkSize = 4096

  sealed trait GetResponse
  case class OkResponse(properties: GetObjectResponse, body: Stream[IO, Byte])
      extends GetResponse
  case object NotModifiedResponse extends GetResponse

  type GetObject = GetObjectRequest => IO[Either[S3Exception, GetResponse]]

  def resource(region: Region): Resource[IO, GetObject] = Resource {
    def response(r: ResponseInputStream[GetObjectResponse]) = OkResponse(
      r.response(),
      fs2.io.readInputStream(IO.pure(r), chunkSize, true)
    )
    def get(
        client: S3Client,
        request: GetObjectRequest
    ): IO[Either[S3Exception, ResponseInputStream[GetObjectResponse]]] =
      IO(Either.catchOnly[S3Exception](client.getObject(request)))
    for c <- IO(S3Client.builder().region(region).build())
    yield (
      (r: GetObjectRequest) =>
        EitherT(get(c, r))
          .map(response)
          .leftFlatMap {
            case e if e.statusCode() == NotModified.code =>
              EitherT.rightT(NotModifiedResponse)
            case e => EitherT.leftT(e)
          }
          .value,
      IO(c.close())
    )
  }
