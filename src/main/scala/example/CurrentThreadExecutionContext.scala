package example

import scala.concurrent.ExecutionContext

object CurrentThreadExecutionContext extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = runnable.run()

  override def reportFailure(cause: Throwable): Unit = {
    cause.printStackTrace()
  }
}
