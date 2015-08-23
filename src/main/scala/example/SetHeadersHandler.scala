package example

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelDuplexHandler, ChannelHandlerContext, ChannelPromise}
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse, HttpHeaders, HttpVersion}
import io.netty.util.AttributeKey

object SetHeadersHandler {

  val DefaultServerName = "long-polling-server-example"
  val ConnectionAttribute =
    AttributeKey.valueOf[String](s"${SetHeadersHandler.getClass.getName}.connection")
  val HttpVersionAttribute =
    AttributeKey.valueOf[HttpVersion](s"${SetHeadersHandler.getClass.getName}.version")

}

@Sharable
class SetHeadersHandler extends ChannelDuplexHandler {

  import SetHeadersHandler._

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case request: FullHttpRequest => {
        val connection = if (HttpHeaders.isKeepAlive(request))
          HttpHeaders.Values.KEEP_ALIVE
        else
          HttpHeaders.Values.CLOSE
        ctx.channel().attr(ConnectionAttribute).set(connection)
        ctx.channel().attr(HttpVersionAttribute).set(request.getProtocolVersion)
      }
      case _ =>
    }

    super.channelRead(ctx, msg)
  }

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {

    msg match {
      case response: FullHttpResponse => {
        response.setProtocolVersion(ctx.channel().attr(HttpVersionAttribute).get())
        response.headers().set(HttpHeaders.Names.SERVER, DefaultServerName)
        response.headers().set(HttpHeaders.Names.CONNECTION, ctx.channel().attr(ConnectionAttribute).get())
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, response.content().readableBytes())
      }
      case _ =>
    }

    super.write(ctx, msg, promise)
  }
}