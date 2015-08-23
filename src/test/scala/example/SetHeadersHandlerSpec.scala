package example

import io.netty.buffer.Unpooled
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http._
import io.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class SetHeadersHandlerSpec extends Specification {

  "handler" >> {

    "sets the necessary headers based in the incoming message" >> {
      val handler = new SetHeadersHandler
      val channel = new EmbeddedChannel(handler)

      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/some-path")
      request
        .headers()
        .set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)

      val content = "some content".getBytes(CharsetUtil.UTF_8)
      val response = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_0,
        HttpResponseStatus.OK,
        Unpooled.wrappedBuffer(content))

      channel.writeInbound(request)
      channel.attr(SetHeadersHandler.ConnectionAttribute).get() must_== (HttpHeaders.Values.KEEP_ALIVE)

      channel.writeOutbound(response)

      response.getProtocolVersion must_== (HttpVersion.HTTP_1_1)
      response.headers().get(HttpHeaders.Names.CONNECTION) must_== (HttpHeaders.Values.KEEP_ALIVE)
      response.headers().get(HttpHeaders.Names.CONTENT_LENGTH) must_== (content.length.toString)
      response.headers().get(HttpHeaders.Names.SERVER) must_== (SetHeadersHandler.DefaultServerName)
    }

    "sets the content length to 0 if there was no content" >> {
      val handler = new SetHeadersHandler
      val channel = new EmbeddedChannel(handler)

      val request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/some-path")
      request
        .headers()
        .set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE)

      val response = new DefaultFullHttpResponse(
        HttpVersion.HTTP_1_1,
        HttpResponseStatus.OK)

      channel.writeInbound(request)
      channel.attr(SetHeadersHandler.ConnectionAttribute).get() must_== (HttpHeaders.Values.CLOSE)

      channel.writeOutbound(response)

      response.getProtocolVersion must_== (HttpVersion.HTTP_1_1)
      response.headers().get(HttpHeaders.Names.CONNECTION) must_== (HttpHeaders.Values.CLOSE)
      response.headers().get(HttpHeaders.Names.CONTENT_LENGTH) must_== "0"
      response.headers().get(HttpHeaders.Names.SERVER) must_== (SetHeadersHandler.DefaultServerName)
    }

  }

}
