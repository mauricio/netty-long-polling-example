package example

import java.util.concurrent.atomic.AtomicInteger

import org.specs2.mutable.Specification
import FutureUtils.awaitFuture

class InitializerSpec extends Specification {

  "initializer" >> {

    "binds and handles requests correctly" >> {
      withInitializer {
        initializer =>
          val getFuture = get(initializer)

          Thread.sleep(500)

          val postResult = awaitFuture(post(initializer))

          postResult.getStatusCode must_==(200)

          val getResult = awaitFuture(getFuture)

          getResult.getStatusCode must_==(200)
          getResult.getResponseBody must_==(sampleBody)
      }
    }

  }

  val path = "/some-path"
  val sampleBody = "sample-body"
  val portCounter = new AtomicInteger(5000)

  def get(initializer : Initializer) =
    Http.get(s"http://localhost:${initializer.port}${path}")

  def post(initializer : Initializer) =
    Http.post(s"http://localhost:${initializer.port}${path}", sampleBody, Map("Content-Type" -> "text/plain"))

  def withInitializer[R]( fn : Initializer => R ) : R = {
    val initializer = new Initializer(10, portCounter.incrementAndGet())(ExecutorServiceUtils.CachedExecutionContext)

    val t = new Thread(new Runnable {
      override def run(): Unit = initializer.start()
    })

    t.start()

    Thread.sleep(500)

    try {
      fn(initializer)
    } finally {
      initializer.stop()
    }
  }

}
