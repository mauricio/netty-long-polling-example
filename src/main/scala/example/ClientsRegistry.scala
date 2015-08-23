package example

import java.util.concurrent.locks.ReentrantLock
import java.util.{Calendar, Date}

import io.netty.channel.ChannelHandlerContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future, Promise}

class ClientsRegistry(timeoutInSeconds: Int) {

  private val lock = new ReentrantLock()
  private val pathsToClients = scala.collection.mutable.Map[String, ListBuffer[ClientKey]]()
  private val orderedClients = scala.collection.mutable.TreeSet[ClientKey]()

  def registerClient(path: String, ctx: ChannelHandlerContext)(implicit executor: ExecutionContext): Future[ClientKey] =
    withLock {
      val client = ClientKey(path, calculateTimeout(), ctx)

      val clients = pathsToClients.getOrElseUpdate(path, ListBuffer[ClientKey]())
      clients += client
      orderedClients += client

      client
    }

  def complete(path: String)(implicit executor: ExecutionContext): Future[Iterable[ClientKey]] =
    withLock {
      pathsToClients.remove(path).map {
        clients =>
          orderedClients --= clients
          clients
      }.getOrElse(Iterable.empty)
    }

  def collectTimeouts()(implicit executor: ExecutionContext): Future[Iterable[ClientKey]] = {
    withLock {
      val iterator = orderedClients.iterator
      val timeouts = ListBuffer[ClientKey]()

      var done = false

      while (iterator.hasNext && !done) {
        val next = iterator.next()
        if (next.isExpired) {
          timeouts += next
        } else {
          done = true
        }
      }

      orderedClients --= timeouts

      timeouts.foreach {
        timeout =>
          pathsToClients.get(timeout.path).foreach(b => b -= timeout)
      }

      timeouts
    }
  }

  def calculateTimeout(): Date = {
    val calendar = Calendar.getInstance
    calendar.add(Calendar.SECOND, timeoutInSeconds)

    calendar.getTime
  }

  private def withLock[R](fn: => R)(implicit executor: ExecutionContext): Future[R] = {
    val p = Promise[R]

    executor.execute(new Runnable {
      override def run(): Unit = {
        lock.lock()
        try {
          p.success(fn)
        } catch {
          case e: Throwable => p.failure(e)
        } finally {
          lock.unlock()
        }
      }
    })

    p.future
  }

}
