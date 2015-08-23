package example

import java.util.{Date, Calendar}

import io.netty.channel.ChannelHandlerContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import FutureUtils.awaitFuture
import ExecutorServiceUtils.CachedExecutionContext

class ClientsRegistrySpec extends Specification with Mockito {

  val path = "sample-path"

  "registry" >> {

    "registers a client and produces it when completing" >> {
      val context = mock[ChannelHandlerContext]
      val registry = new ClientsRegistry(1)

      val client = awaitFuture(registry.registerClient(path, context))

      val result = awaitFuture(registry.complete(path)).toList

      result.length must_==(1)
      result(0) must_==(client)

      awaitFuture(registry.complete(path)).toList.length must_==(0)
    }

    "returns an empty collection if no clients were there to be collected" >> {
      val registry = new ClientsRegistry(1)
      awaitFuture(registry.complete(path)) must beEmpty
    }

    "removes from timeouts once completed" >> {
      val context = mock[ChannelHandlerContext]
      val registry = new ClientsRegistry(0)

      val client = awaitFuture(registry.registerClient(path, context))
      val result = awaitFuture(registry.complete(path)).toList

      result.length must_==(1)
      result(0) must_==(client)

      Thread.sleep(500)

      awaitFuture(registry.collectTimeouts()) must beEmpty
    }

    "timeouts clients and removes them from the collection" >> {
      var date = new Date()

      val registry = new ClientsRegistry(0) {
        override def calculateTimeout(): Date = date
      }

      val timeoutedChannel = mock[ChannelHandlerContext]
      val timeoutedClient = awaitFuture(registry.registerClient(path, timeoutedChannel))

      val futureChannel = mock[ChannelHandlerContext]
      val futureTime = Calendar.getInstance
      futureTime.add(Calendar.DATE, 1)

      date = futureTime.getTime

      val futureClient = awaitFuture(registry.registerClient(path, futureChannel))

      Thread.sleep(500)

      val timeouts = awaitFuture(registry.collectTimeouts()).toList

      timeouts.length must_==(1)
      timeouts(0) must_==(timeoutedClient)

      val result = awaitFuture(registry.complete(path)).toList

      result.length must_==(1)
      result(0) must_==(futureClient)
    }

  }

}
