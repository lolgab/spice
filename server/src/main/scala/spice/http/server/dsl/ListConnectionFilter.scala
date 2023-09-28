package spice.http.server.dsl

import cats.effect.IO
import scribe.mdc.MDC
import spice.http.HttpExchange

case class ListConnectionFilter(filters: List[ConnectionFilter]) extends ConnectionFilter {
  override def apply(exchange: HttpExchange)
                    (implicit mdc: MDC): IO[FilterResponse] = firstPath(exchange, filters)

  private def firstPath(exchange: HttpExchange,
                        filters: List[ConnectionFilter])
                       (implicit mdc: MDC): IO[FilterResponse] = if (filters.isEmpty) {
    IO.pure(FilterResponse.Stop(exchange))
  } else {
    val filter = filters.head
    filter.apply(exchange).flatMap {
      case r: FilterResponse.Continue => firstPath(r.exchange, filters.tail)
      case r: FilterResponse.Stop => firstPath(r.exchange, filters.tail)
    }
  }
}