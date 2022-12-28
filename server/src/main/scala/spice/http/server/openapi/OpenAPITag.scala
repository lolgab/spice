package spice.http.server.openapi

import fabric.rw.RW

case class OpenAPITag(name: String)

object OpenAPITag {
  implicit val rw: RW[OpenAPITag] = RW.gen
}