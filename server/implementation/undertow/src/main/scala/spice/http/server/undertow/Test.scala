package spice.http.server.undertow

import cats.effect.{ExitCode, IO, IOApp}
import moduload.Moduload
import spice.http.content.Content
import spice.http.HttpConnection
import spice.http.server.HttpServer
import spice.net.ContentType

object Test extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    Moduload.load()

    val server = new HttpServer {
      override def handle(connection: HttpConnection): IO[HttpConnection] = IO {
        connection.modify { response =>
          response.withContent(Content.string("Hello, Spice! It's nice!", ContentType.`text/plain`))
        }
      }
    }
    server.start().flatMap { _ =>
      scribe.info("Server started!")
      server.whileRunning().map(_ => ExitCode.Success)
    }
  }
}