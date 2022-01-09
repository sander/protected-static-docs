package nl.sanderdijkhuis.docs

import cats.effect.IO
import cats.effect.kernel.Resource
import fs2.Stream
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  GetObjectResponse,
  S3Exception
}

trait ObjectRepository:

  def getObject(
      request: GetObjectRequest
  ): IO[Either[S3Exception, ObjectRepository.GetResponse]]

  def close(): IO[Unit]

object ObjectRepository:

  val chunkSize = 4096

  case class GetResponse(properties: GetObjectResponse, body: Stream[IO, Byte])

  def resource(region: Region) = Resource.make(
    IO(S3Client.builder().region(region).build()).map(c =>
      new ObjectRepository:
        override def getObject(request: GetObjectRequest) =
          IO(c.getObject(request))
            .map(r =>
              GetResponse(
                r.response(),
                fs2.io.readInputStream(IO.pure(r), chunkSize, true)
              )
            )
            .map(Right.apply)
            .handleErrorWith {
              case e: S3Exception => IO.pure(Left(e))
              case e              => IO.raiseError(e)
            }

        override def close(): IO[Unit] = IO(c.close())
    )
  )(_.close())
