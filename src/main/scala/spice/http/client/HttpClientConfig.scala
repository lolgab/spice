package spice.http.client

import reactify.Var
import spice.http.client.intercept.Interceptor
import spice.net.DNS

import scala.concurrent.duration._

case class HttpClientConfig(retries: Int = 0,
                            retryDelay: FiniteDuration = 5.seconds,
                            interceptor: Interceptor = Interceptor.empty,
                            connectionPool: ConnectionPool = ConnectionPool.default,
                            saveDirectory: String = ClientPlatform.defaultSaveDirectory,
                            timeout: FiniteDuration = 15.seconds,
                            pingInterval: Option[FiniteDuration] = None,
                            dns: DNS = DNS.default,
                            dropNullValuesInJson: Boolean = false,
                            sessionManager: Option[SessionManager] = None,
                            failOnHttpStatus: Boolean = true,
                            validateSSLCertificates: Boolean = true) {
  def retries(retries: Int): HttpClientConfig = copy(retries = retries)
  def retryDelay(retryDelay: FiniteDuration): HttpClientConfig = copy(retryDelay = retryDelay)
  def interceptor(interceptor: Interceptor): HttpClientConfig = copy(interceptor = interceptor)
  def connectionPool(connectionPool: ConnectionPool): HttpClientConfig = copy(connectionPool = connectionPool)
  def saveDirectory(saveDirectory: String): HttpClientConfig = copy(saveDirectory = saveDirectory)
  def timeout(timeout: FiniteDuration): HttpClientConfig = copy(timeout = timeout)
  def pingInterval(pingInterval: Option[FiniteDuration]): HttpClientConfig = copy(pingInterval = pingInterval)
  def dns(dns: DNS): HttpClientConfig = copy(dns = dns)
  def sessionManager(sessionManager: SessionManager): HttpClientConfig = copy(sessionManager = Some(sessionManager))
  def session(session: Session): HttpClientConfig = copy(sessionManager = Some(new SessionManager(session)))
  def failOnHttpStatus(failOnHttpStatus: Boolean): HttpClientConfig = copy(failOnHttpStatus = failOnHttpStatus)
}

object HttpClientConfig {
  /**
    * Note: This must be configured before getting the HttpClient or the implementation-specific configuration will be
    * already set.
    */
  val default: Var[HttpClientConfig] = Var(HttpClientConfig())
}