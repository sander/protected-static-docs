package nl.sanderdijkhuis.docs

import cats.effect.IO
import cats.effect.kernel.Resource
import munit.CatsEffectSuite
import org.http4s.blaze.client.*
import org.http4s.client.*

import scala.concurrent.ExecutionContext.global

class MainSuite extends CatsEffectSuite {
  test("run a web server") {
    val app = Resource.make(Main.run.start)(_.cancel)
    val client = BlazeClientBuilder[IO](global).resource
    val url = s"http://localhost:${Main.port}"
    app.use { _ => client.use(_.expect[String](url)) }
  }
}
