package spice.ajax

import cats.effect.IO
import org.scalajs.dom
import org.scalajs.dom.XMLHttpRequest
import scribe.Logging
import spice.http.HttpMethod
import spice.http.content.FormData
import spice.net.URL

import scala.collection.immutable.Queue
import scala.scalajs.js.|
import scala.util.Try

class AjaxManager(val maxConcurrent: Int) extends Logging {
  private var _queue = Queue.empty[AjaxAction]
  private var _running = Set.empty[AjaxAction]

  def queue: Int = _queue.size
  def running: Int = _running.size

  def enqueue(url: URL,
              method: HttpMethod = HttpMethod.Post,
              data: Option[FormData | String] = None,
              timeout: Int = 0,
              headers: Map[String, String] = Map.empty,
              withCredentials: Boolean = true,
              responseType: String = ""): AjaxAction = {
    val request = new AjaxRequest(url, method, data, timeout, headers, withCredentials, responseType)
    val action = new AjaxAction(request)
    enqueue(action)
    action
  }

  def enqueue(action: AjaxAction): IO[Try[XMLHttpRequest]] = {
    _queue = _queue.enqueue(action)
    action._state @= ActionState.Enqueued
    checkQueue()
    action.io
  }

  def checkQueue(): Unit = if (_running.size < maxConcurrent && _queue.nonEmpty) {
    val (action, updated) = _queue.dequeue
    _queue = updated
    _running += action
    action.start(this)
  }

  private[ajax] def remove(action: AjaxAction): Unit = {
    _running -= action
    checkQueue()
  }
}