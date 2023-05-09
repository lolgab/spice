package spice.http.server.undertow

import cats.effect.IO
import io.undertow.server.HttpServerExchange
import io.undertow.server.handlers.form.{FormDataParser, FormParserFactory}
import io.undertow.util.HeaderMap
import org.xnio.streams.ChannelInputStream
import spice.http.content.{Content, FormData, FormDataContent, FormDataEntry, StreamContent, StringContent}
import spice.http.content.FormDataEntry.{FileEntry, StringEntry}
import spice.http.{Headers, HttpMethod, HttpRequest}
import spice.net.{ContentType, IP, URL}
import scala.jdk.CollectionConverters.IterableHasAsScala

object UndertowRequestParser {
  private val formParserBuilder = FormParserFactory.builder()

  def apply(exchange: HttpServerExchange, url: URL): IO[HttpRequest] = IO {
    val source = IP
      .fromString(exchange.getSourceAddress.getAddress.getHostAddress)
      .getOrElse {
        scribe.warn(s"Invalid IP address: ${exchange.getSourceAddress.getAddress.getHostAddress}")
        IP.v4(0, 0, 0, 0)
      }
    val headers = parseHeaders(exchange.getRequestHeaders)

    val content: Option[Content] = if (exchange.getRequestContentLength > 0L) {
      Headers.`Content-Type`.value(headers).getOrElse(ContentType.`text/plain`) match {
        case ContentType.`multipart/form-data` =>
          exchange.startBlocking()
          val formDataParser = formParserBuilder.build().createParser(exchange)
          formDataParser.parseBlocking()
          val formData = exchange.getAttachment(FormDataParser.FORM_DATA)
          val data = formData.asScala.toList.map { key =>
            val entries: List[FormDataEntry] = formData.get(key).asScala.map { entry =>
              val headers = parseHeaders(entry.getHeaders)
              if (entry.isFileItem) {
                val path = entry.getFileItem.getFile
                FileEntry(entry.getFileName, path.toFile, headers)
              } else {
                StringEntry(entry.getValue, headers)
              }
            }.toList
            FormData(key, entries)
          }
          Some(FormDataContent(data))
        case ct =>
          val stream = fs2.io.readInputStream[IO](
            fis = IO(new ChannelInputStream(exchange.getRequestChannel)),
            chunkSize = 1024
          )
          Some(StreamContent(stream, ct))
      }
    } else {
      None
    }

    HttpRequest(
      method = HttpMethod(exchange.getRequestMethod.toString),
      source = source,
      url = url,
      headers = headers,
      content = content
    )
  }

  private def parseHeaders(headerMap: HeaderMap): Headers = Headers(headerMap.asScala.map { hv =>
    hv.getHeaderName.toString -> hv.asScala.toList
  }.toMap)
}