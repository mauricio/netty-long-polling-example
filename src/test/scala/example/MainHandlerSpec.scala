package example

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import org.specs2.mutable.Specification

import scala.concurrent.{Future, ExecutionContext}

class MainHandlerSpec extends Specification {

  val path = "/some-path"
  val contents = "some-contents"
  val contentBytes = contents.getBytes(CharsetUtil.UTF_8)

  "handler" >> {

    "registers the client and sends the response back" >> {
      val registry = new ClientsRegistry(1)
      val handler = new MainHandler(registry)(CurrentThreadExecutionContext)
      val requesterChannel = new EmbeddedChannel(handler)
      val notifierChannel = new EmbeddedChannel(handler)

      val pollRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
      requesterChannel.writeInbound(pollRequest)

      val notificationRequest = new DefaultFullHttpRequest(
        HttpVersion.HTTP_1_1, HttpMethod.POST, path, Unpooled.wrappedBuffer(contentBytes))
      notifierChannel.writeInbound(notificationRequest)

      val pollResponse = requesterChannel.readOutbound().asInstanceOf[FullHttpResponse]

      pollResponse.getStatus must_==(HttpResponseStatus.OK)
      pollResponse.content().toString(CharsetUtil.UTF_8) must_==(contents)

      val notificationResponse = notifierChannel.readOutbound().asInstanceOf[FullHttpResponse]
      notificationResponse.getStatus must_==(HttpResponseStatus.OK)
    }

    "timeouts clients and sends them a response" >> {
      val registry = new ClientsRegistry(0)
      val handler = new MainHandler(registry)(CurrentThreadExecutionContext)
      val channel = new EmbeddedChannel(handler)

      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
      channel.writeInbound(request)

      Thread.sleep(500)

      handler.evaluateTimeouts()

      val response = channel.readOutbound().asInstanceOf[FullHttpResponse]

      response.getStatus must_==(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      response.content().toString(CharsetUtil.UTF_8) must_==("channel timeouted without a response")
    }

    "returns a 404 for other requests" >> {
      val registry = new ClientsRegistry(1)
      val handler = new MainHandler(registry)(CurrentThreadExecutionContext)
      val channel = new EmbeddedChannel(handler)

      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, path)
      channel.writeInbound(request)

      val response = channel.readOutbound().asInstanceOf[FullHttpResponse]

      response.getStatus must_==(HttpResponseStatus.NOT_FOUND)
    }

    "returns an error if it can't register the client" >> {
      val exception = new IllegalStateException("can't register clients right now, sorry")
      val registry = new ClientsRegistry(1) {
        override def registerClient(path: String, ctx: ChannelHandlerContext)(implicit executor: ExecutionContext):
        Future[ClientKey] = Future.failed(exception)
      }

      val handler = new MainHandler(registry)(CurrentThreadExecutionContext)
      val channel = new EmbeddedChannel(handler)

      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path)
      channel.writeInbound(request)

      val response = channel.readOutbound().asInstanceOf[FullHttpResponse]
      response.getStatus must_==(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      response.content().toString(CharsetUtil.UTF_8) must_==(exception.getMessage)
    }

    "returns an error if it can't notify clients" >> {
      val exception = new IllegalStateException("can't notify clients right now, sorry")
      val registry = new ClientsRegistry(1) {
        override def complete(path: String)(implicit executor: ExecutionContext):
        Future[Iterable[ClientKey]] = Future.failed(exception)
      }

      val handler = new MainHandler(registry)(CurrentThreadExecutionContext)
      val channel = new EmbeddedChannel(handler)

      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path)
      channel.writeInbound(request)

      val response = channel.readOutbound().asInstanceOf[FullHttpResponse]
      response.getStatus must_==(HttpResponseStatus.INTERNAL_SERVER_ERROR)
      response.content().toString(CharsetUtil.UTF_8) must_==(exception.getMessage)
    }

  }


}
