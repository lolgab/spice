package spice.http.content

import spice.net.ContentType

import java.io.File
import java.net.URL

trait ContentHelpers extends SharedContentHelpers {
  private def unsupported: Content = throw new UnsupportedOperationException("Not supported in Scala.js")
  
  override def file(file: File): Content = unsupported
  override def file(file: File, contentType: ContentType): Content = unsupported
  override def url(url: URL): Content = unsupported
  override def url(url: URL, contentType: ContentType): Content = unsupported
  override def classPath(url: URL): Content = unsupported
  override def classPath(path: String, contentType: ContentType): Content = unsupported
}