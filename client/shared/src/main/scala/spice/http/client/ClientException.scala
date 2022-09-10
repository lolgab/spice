package spice.http.client

import spice.http.{HttpRequest, HttpResponse, HttpStatus}

class ClientException(val message: String,
                      val request: HttpRequest,
                      val response: HttpResponse,
                      val cause: Option[Throwable]) extends RuntimeException(message, cause.orNull) {
  def status: HttpStatus = response.status
}