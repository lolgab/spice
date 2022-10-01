package spice.http.server

import cats.effect.IO
import cats.effect.unsafe.{IORuntime, IORuntimeConfig, Scheduler}
import scribe.cats.{io => logger}
import spice.http.HttpConnection
import spice.{ErrorSupport, ImplementationManager, Initializable}

import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}
import java.util.concurrent.{Executors, ThreadFactory}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait HttpServer extends HttpHandler with Initializable with ErrorSupport {
  val config = new ServerConfig(this)

  protected val defaultExecutionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool(new ThreadFactory {
    private val counter = new AtomicLong(0L)

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setName(s"${config.name}-${counter.incrementAndGet()}")
      t.setDaemon(true)
      t
    }
  }))
  IORuntime.createDefaultScheduler()
  protected def computeExecutionContext: ExecutionContext = defaultExecutionContext
  protected def blockingExecutionContext: ExecutionContext = defaultExecutionContext
  protected val (scheduler, shutdownScheduler) = IORuntime.createDefaultScheduler(s"${config.name}-scheduler")
  lazy val ioRuntime: IORuntime = IORuntime(
    compute = computeExecutionContext,
    blocking = blockingExecutionContext,
    scheduler = scheduler,
    shutdown = shutdownScheduler,
    config = IORuntimeConfig()
  )

  private val implementation = HttpServerImplementationManager(this)

  def isRunning: Boolean = isInitialized && implementation.isRunning

  /**
   * Init is called on start(), but only the first time. If the server is restarted it is not invoked again.
   */
  override protected def initialize(): IO[Unit] = IO.unit

  def start(): IO[Unit] = for {
    _ <- init()
    _ <- implementation.start(this)
    _ <- logger.info(s"Server started on ${config.enabledListeners.mkString(", ")}")
  } yield {
    ()
  }

  def errorRecovery(connection: HttpConnection, throwable: Throwable): IO[HttpConnection] = IO.pure(connection)

  def stop(): IO[Unit] = implementation.stop(this)

  def restart(): Unit = synchronized {
    stop()
    start()
  }

  def whileRunning(delay: FiniteDuration = 1.second): IO[Unit] = if (isRunning) {
    IO.sleep(delay).flatMap(_ => whileRunning(delay))
  } else {
    IO.unit
  }

  def dispose(): Unit = stop()
}

trait HttpServerImplementation {
  def isRunning: Boolean

  def start(server: HttpServer): IO[Unit]
  def stop(server: HttpServer): IO[Unit]
}

object HttpServerImplementationManager extends ImplementationManager[HttpServerImplementation, HttpServer]