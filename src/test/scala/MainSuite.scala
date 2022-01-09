package nl.sanderdijkhuis.docs

import cats.data.EitherT
import cats.effect.IO
import cats.effect.kernel.Resource
import cats.implicits._
import munit.CatsEffectSuite
import org.http4s.blaze.client.*
import org.http4s.client.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3
import software.amazon.awssdk.services.s3.model.{
  GetObjectRequest,
  GetObjectResponse,
  S3Exception
}
import fs2.Stream

import java.io.InputStream
import java.nio.charset.StandardCharsets
import scala.concurrent.ExecutionContext.global

class MainSuite extends CatsEffectSuite {
  val client = BlazeClientBuilder[IO](global).resource

  test("run a web server") {
    val app = Resource.make(Main.run.start)(_.cancel)
    val url = s"http://localhost:${Main.port}"
    app.use { _ => client.use(_.expect[String](url)) }
  }

  test("get from s3 (system integration test)") {
    assume(
      System.getenv("AWS_ACCESS_KEY_ID") == "AKIAY6EFGFEFHMASZ7L4",
      "only run system integration test with proper access"
    )

    val request = GetObjectRequest
      .builder()
      .bucket("test-static-docs")
      .key("index.html")
      .build()

    ObjectRepository.resource(Region.EU_CENTRAL_1).use { getObject =>
      for
        r <- EitherT(getObject(request)).valueOrF(IO.raiseError)
        b <- r.body.compile.to(Array)
        s <- IO(new String(b, StandardCharsets.UTF_8))
      yield
        assert(s.indexOf("hello world") != -1)
        assert(r.properties.contentType() == "text/html")
    }
  }
}
