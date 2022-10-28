package spec

import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import spice.http.server.{DefaultErrorHandler, HttpServer, MutableHttpServer}
import spice.http.{HttpExchange, HttpMethod, HttpRequest, HttpStatus}
import spice.http.server.dsl._
import spice.net._

class ServerDSLSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {
  private lazy val text = "Hello, World!".withContentType(ContentType.`text/plain`)
  private lazy val html = """<html>
    <head>
      <title>Hello, World!</title>
    </head>
    <body>
      <h1>Hello, World!</h1>
    </body>
  </html>""".withContentType(ContentType.`text/html`)

  "Server DSL" when {
    "creating a simple handler" should {
      "properly accept a request for /hello/world.txt" in {
        val request = HttpRequest(source = ip"127.0.0.1", url = url"http://www.example.com/hello/world.txt")
        server.handle(HttpExchange(request)).map { exchange =>
          val response = exchange.response
          response.content should be(Some(text))
          response.status should be(HttpStatus.OK)
        }
      }
      "properly accept a request for /hello/world.html" in {
        val request = HttpRequest(source = ip"127.0.0.1", url = url"http://www.example.com/hello/world.html")
        server.handle(HttpExchange(request)).map { exchange =>
          val response = exchange.response
          response.content should be(Some(html))
          response.status should be(HttpStatus.OK)
        }
      }
      "properly return a 404 for /hello/other.html" in {
        val request = HttpRequest(source = ip"127.0.0.1", url = url"http://www.example.com/hello/other.html")
        server.handle(HttpExchange(request)).map { exchange =>
          val response = exchange.response
          response.status should be(HttpStatus.NotFound)
        }
      }
      "properly return a 404 for a POST" in {
        val request = HttpRequest(
          source = ip"127.0.0.1",
          url = url"http://www.example.com/hello/world.html",
          method = HttpMethod.Post
        )
        server.handle(HttpExchange(request)).map { exchange =>
          val response = exchange.response
          response.status should be(HttpStatus.NotFound)
        }
      }
      "reject a request from a different origin IP" in {
        val request = HttpRequest(source = ip"127.0.0.2", url = url"http://www.example.com/hello/world.html")
        server.handle(HttpExchange(request)).map { exchange =>
          val response = exchange.response
          response.content should be(Some(DefaultErrorHandler.html(HttpStatus.NotFound)))
          response.status should be(HttpStatus.NotFound)
        }
      }
    }
  }

  object server extends MutableHttpServer {
    handler(
      allow(ip"127.0.0.1", ip"192.168.1.1") / HttpMethod.Get / "hello" / List(
        "world.txt" / text,
        "world.html" / html
      )
    )
  }
}