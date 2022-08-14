package spice.http

import spice.http.cookie._

object CookieHeader extends ListTypedHeaderKey[RequestCookie] {
  override def key: String = "Cookie"
  override protected def commaSeparated: Boolean = false

  override def value(headers: Headers): List[RequestCookie] = {
    val cookies = headers.get(this)
    try {
      cookies.flatMap(_.split(';')).map(_.trim).collect {
        case SetCookie.KeyValueRegex(key, value) => RequestCookie(key, value)
      }
    } catch {
      case t: Throwable => {
        scribe.error(new RuntimeException(s"Failed to parse cookie: [${cookies.mkString("|")}]", t))
        throw t
      }
    }
  }

  override def apply(value: RequestCookie): Header = Header(this, value.http)
}