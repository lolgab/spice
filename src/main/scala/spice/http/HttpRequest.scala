package spice.http

import spice.http.content.Content
import spice.http.cookie.RequestCookie
import spice.net.{IP, URL}

case class HttpRequest(method: HttpMethod = HttpMethod.Get,
                       source: IP = IP.v4.LocalHost,
                       url: URL = URL(),
                       headers: Headers = Headers.empty,
                       content: Option[Content] = None,
                       timestamp: Long = System.currentTimeMillis()) {
  lazy val cookies: List[RequestCookie] = Headers.Request.`Cookie`.value(headers)
  def withHeader(header: Header): HttpRequest = copy(headers = headers.withHeader(header))
  def withHeader(key: String, value: String): HttpRequest = copy(headers = headers.withHeader(key, value))
  def withContent(content: Content): HttpRequest = copy(content = Some(content))
  def originalSource: IP = headers.first(Headers.Request.`X-Forwarded-For`).map {
      case s if s.indexOf(',') != -1 => s.substring(0, s.indexOf(','))
      case s => s
    }.flatMap(s => IP.fromString(s)).getOrElse(source)
}