package example

import java.util.concurrent.{Executors, ThreadFactory}

import scala.concurrent.ExecutionContext

object DaemonThreadFactory  extends ThreadFactory {

  def newThread(r: Runnable): Thread = {
    val thread = Executors.defaultThreadFactory().newThread(r)
    thread.setDaemon(true)
    thread
  }

}

object ExecutorServiceUtils {

  implicit val CachedThreadPool = Executors.newCachedThreadPool(DaemonThreadFactory)
  implicit val CachedExecutionContext = ExecutionContext.fromExecutor(CachedThreadPool)

}
