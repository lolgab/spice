package spice.http.client

import cats.effect.unsafe.implicits.global
import cats.effect.{Deferred, IO}
import moduload.Moduload
import spice.http._
import spice.http.content.FormDataEntry.{FileEntry, StringEntry}
import spice.http.content._
import spice.net.ContentType
import spice.stream
import spice.stream._

import java.io.{File, IOException}
import java.net.{InetAddress, Socket}
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl._
import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

/**
  * Asynchronous HttpClient for simple request response support.
  *
  * Adds support for simple restful request/response JSON support.
  */
class OkHttpClientImplementation(config: HttpClientConfig) extends HttpClientImplementation(config) {
  private lazy val client = {
    val b = new okhttp3.OkHttpClient.Builder()
    b.sslSocketFactory(new SSLSocketFactory {
//      private val default = SSLSocketFactory.getDefault.asInstanceOf[SSLSocketFactory]
      private val disabled = {
        val trustAllCerts = Array[TrustManager](new X509TrustManager {
          override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

          override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

          override def getAcceptedIssuers: Array[X509Certificate] = null
        })
        val sc = SSLContext.getInstance("SSL")
        sc.init(null, trustAllCerts, new SecureRandom)
        sc.getSocketFactory
      }
      private def f: SSLSocketFactory = disabled //if (config.validateSSLCertificates) default else disabled

      override def getDefaultCipherSuites: Array[String] = f.getDefaultCipherSuites

      override def getSupportedCipherSuites: Array[String] = f.getSupportedCipherSuites

      override def createSocket(socket: Socket, s: String, i: Int, b: Boolean): Socket = f.createSocket(socket, s, i, b)

      override def createSocket(s: String, i: Int): Socket = f.createSocket(s, i)

      override def createSocket(s: String, i: Int, inetAddress: InetAddress, i1: Int): Socket = f.createSocket(s, i, inetAddress, i1)

      override def createSocket(inetAddress: InetAddress, i: Int): Socket = f.createSocket(inetAddress, i)

      override def createSocket(inetAddress: InetAddress, i: Int, inetAddress1: InetAddress, i1: Int): Socket = f.createSocket(inetAddress, i, inetAddress1, i1)
    }, new X509TrustManager {
//      private val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm)
//      private val default = {
//        factory.init(KeyStore.getInstance(KeyStore.getDefaultType))
//        factory.getTrustManagers.apply(0).asInstanceOf[X509TrustManager]
//      }

      override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {
        if (config.validateSSLCertificates) {
//          default.checkClientTrusted(x509Certificates, s)
        }
      }

      override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {
        if (config.validateSSLCertificates) {
//          default.checkServerTrusted(x509Certificates, s)
        }
      }

      override def getAcceptedIssuers: Array[X509Certificate] = //if (config.validateSSLCertificates) {
//        default.getAcceptedIssuers
//      } else {
        Array.empty[X509Certificate]
//      }
    })
    b.hostnameVerifier(new HostnameVerifier {
      override def verify(s: String, sslSession: SSLSession): Boolean = true
    })
    b.connectTimeout(config.timeout.toMillis, TimeUnit.MILLISECONDS)
    b.readTimeout(config.timeout.toMillis, TimeUnit.MILLISECONDS)
    b.writeTimeout(config.timeout.toMillis, TimeUnit.MILLISECONDS)
    b.dns((hostname: String) => {
      val list = new util.ArrayList[InetAddress]()
      config.dns.lookup(hostname).unsafeRunSync() match {
        case Some(ip) => list.add(InetAddress.getByAddress(ip.address.map(_.toByte).toArray))
        case None => // None
      }
      list
    })
    config.pingInterval.foreach(d => b.pingInterval(d.toMillis, TimeUnit.MILLISECONDS))
    b.build()
  }

  /*def disableSSLVerification(): Unit = {
    val trustAllCerts = Array[TrustManager](new X509TrustManager {
      override def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

      override def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String): Unit = {}

      override def getAcceptedIssuers: Array[X509Certificate] = null
    })
    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts, new SecureRandom)
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory)
    val allHostsValid = new HostnameVerifier {
      override def verify(s: String, sslSession: SSLSession): Boolean = true
    }
    HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
  }*/

  override def send(request: HttpRequest): IO[Try[HttpResponse]] = Deferred[IO, Try[HttpResponse]].flatMap { deferred =>
    val req = requestToOk(request)
    client.newCall(req).enqueue(new okhttp3.Callback {
      override def onResponse(call: okhttp3.Call, res: okhttp3.Response): Unit = {
        val response = responseFromOk(res)
        deferred.complete(Success(response))
      }

      override def onFailure(call: okhttp3.Call, exc: IOException): Unit = deferred.complete(Failure(exc))
    })
    OkHttpClientImplementation.process(deferred.get)
  }

  private def requestToOk(request: HttpRequest): okhttp3.Request = {
    val r = new okhttp3.Request.Builder().url(request.url.toString)

    // Headers
    request.headers.map.foreach {
      case (key, values) => values.foreach(r.addHeader(key, _))
    }

    def ct(contentType: ContentType): okhttp3.MediaType = okhttp3.MediaType.parse(contentType.outputString)

    // Content
    val body = request.content.map {
      case StringContent(value, contentType, _) => okhttp3.RequestBody.create(value, ct(contentType))
      case FileContent(file, contentType, _) => okhttp3.RequestBody.create(file, ct(contentType))
      case BytesContent(array, contentType, _) => okhttp3.RequestBody.create(array, ct(contentType))
      case FormDataContent(data) => {
        val form = new okhttp3.MultipartBody.Builder()
        form.setType(ct(ContentType.`multipart/form-data`))
        data.foreach {
          case FormData(key, entries) => entries.foreach {
            case StringEntry(value, _) => form.addFormDataPart(key, value)
            case FileEntry(fileName, file, headers) => {
              val partType = Headers.`Content-Type`.value(headers).getOrElse(ContentType.`application/octet-stream`)
              val part = okhttp3.RequestBody.create(file, ct(partType))
              form.addFormDataPart(key, fileName, part)
            }
          }
        }
        form.build()
      }
      case c => throw new RuntimeException(s"Unsupported request content: $c")
    }.getOrElse {
      if (request.method != HttpMethod.Get) {
        okhttp3.RequestBody.create("", None.orNull)
      } else {
        None.orNull
      }
    }

    // Method
    r.method(request.method.value, body).header("Content-Length", Option(body).map(_.contentLength().toString).getOrElse("0"))
    r.build()
  }

  private def responseFromOk(r: okhttp3.Response): HttpResponse = {
    // Status
    val status = HttpStatus(code = r.code(), message = r.message())

    // Headers
    val headersMap = r.headers().names().asScala.toList.map { key =>
      key -> r.headers(key).asScala.toList
    }.toMap
    val headers = Headers(headersMap)

    // Content
    val contentType = Headers.`Content-Type`.value(headers).getOrElse(ContentType.`application/octet-stream`)
    val contentLength = Headers.`Content-Length`.value(headers)
    val content = Option(r.body()).map { responseBody =>
      if (contentToString(contentType, contentLength)) {
        Content.string(responseBody.string(), contentType)
      } else if (contentToBytes(contentType, contentLength)) {
        Content.bytes(responseBody.bytes(), contentType)
      } else {
        val suffix = contentType.extension.getOrElse("client")
        val file = File.createTempFile("youi", s".$suffix", new File(config.saveDirectory))
        stream.Stream.apply(responseBody.byteStream(), file)
        Content.file(file, contentType)
      }
    }

    HttpResponse(
      status = status,
      headers = headers,
      content = content
    )
  }

  override def connectionPool(maxIdleConnections: Int, keepAlive: FiniteDuration): ConnectionPool =
    OkHttpConnectionPool(maxIdleConnections, keepAlive)

  protected def contentToString(contentType: ContentType, contentLength: Option[Long]): Boolean = {
    contentType.`type` == "text" || contentType.subType == "json"
  }


  override def content2String(content: Content): String = content match {
    case c: StringContent => c.value
    case c: BytesContent => String.valueOf(c.value)
    case c: FileContent => stream.Stream.apply(c.file, new mutable.StringBuilder).toString
    case _ => throw new RuntimeException(s"$content not supported")
  }

  protected def contentToBytes(contentType: ContentType, contentLength: Option[Long]): Boolean = {
    contentLength.exists(l => l > 0L && l < 512000L)
  }

  protected def content2Bytes(content: Content): Array[Byte] = content match {
    case c: StringContent => c.value.getBytes("UTF-8")
    case c: BytesContent => c.value
    case c: FileContent => stream.Stream.apply(c.file, new mutable.StringBuilder).toString.getBytes("UTF-*")
    case _ => throw new RuntimeException(s"$content not supported")
  }

  def logStats(): Unit = {
    val g = OkHttpClientImplementation
    scribe.info(s"HttpClient stats - Pool[active: ${config.connectionPool.active}, idle: ${config.connectionPool.idle}, total: ${config.connectionPool.total}], Global[active: ${g.active}, successful: ${g.successful}, failure: ${g.failure}, total: ${g.total}]")
  }
}

object OkHttpClientImplementation extends Moduload {
  private val cached = new ConcurrentHashMap[HttpClientConfig, HttpClientImplementation]

  private[client] val _total = new AtomicLong(0L)
  private[client] val _active = new AtomicLong(0L)
  private[client] val _successful = new AtomicLong(0L)
  private[client] val _failure = new AtomicLong(0L)

  override def load(): Unit = {
    scribe.info(s"Registering OkHttpClientImplementation...")
    HttpClientImplementationManager.register(creator)
  }

  override def error(t: Throwable): Unit = {
    scribe.error("Error while attempting to register OkHttpClientImplementation", t)
  }

  private def creator(config: HttpClientConfig): HttpClientImplementation =
    cached.computeIfAbsent(config, config => new OkHttpClientImplementation(config))

  private[client] def process(io: IO[Try[HttpResponse]]): IO[Try[HttpResponse]] = {
    _total.incrementAndGet()
    _active.incrementAndGet()
    io.flatMap { t =>
      IO {
        t match {
          case Success(_) =>
            _successful.incrementAndGet()
            _active.decrementAndGet()
          case Failure(_) =>
            _failure.incrementAndGet()
            _active.decrementAndGet()
        }
      }.map(_ => t)
    }
  }

  def total: Long = _total.get()
  def active: Long = _active.get()
  def successful: Long = _successful.get()
  def failure: Long = _failure.get()
}