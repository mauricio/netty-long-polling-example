package example

import java.util.concurrent.TimeoutException

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.util.{CharsetUtil, ReferenceCountUtil}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object MainHandler {

  val log = Log.get[MainHandler]

}

@Sharable
class MainHandler( registry : ClientsRegistry )(implicit executor: ExecutionContext)
  extends SimpleChannelInboundHandler[FullHttpRequest] {

  import MainHandler.log

  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {

    log.info(s"Received request ${msg}")

    msg.getMethod match {
      case HttpMethod.GET => {
        registry.registerClient(msg.getUri, ctx).onFailure {
          case e => writeError(ctx, e)
        }
      }
      case HttpMethod.POST => {
        ReferenceCountUtil.retain(msg)
        registry.complete(msg.getUri).onComplete {
          result =>
            try {
              result match {
                case Success(clients) => {
                  clients.foreach {
                    client =>
                      client.ctx.writeAndFlush(buildResponse(msg))
                  }
                  ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK))
                }
                case Failure(e) =>
                  writeError(ctx, e)
              }
            } finally {
              ReferenceCountUtil.release(msg)
            }
        }
      }
      case _ =>
        ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND))
    }
  }

  def evaluateTimeouts(): Unit = {
    registry.collectTimeouts().onSuccess {
      case clients => clients.foreach {
        client =>
          writeError(client.ctx, new TimeoutException("channel timeouted without a response"))
      }
    }
  }

  def writeError(ctx : ChannelHandlerContext, e : Throwable): Unit = {
    val response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.INTERNAL_SERVER_ERROR,
      Unpooled.wrappedBuffer(e.getMessage.getBytes(CharsetUtil.UTF_8))
    )

    response.headers().add(HttpHeaders.Names.CONTENT_TYPE, "text/plain")

    ctx.writeAndFlush(response)
  }

  def buildResponse( request : FullHttpRequest ) : FullHttpResponse = {
    val response = new DefaultFullHttpResponse(
      HttpVersion.HTTP_1_1,
      HttpResponseStatus.OK,
      Unpooled.copiedBuffer(request.content())
    )

    if ( request.headers().contains(HttpHeaders.Names.CONTENT_TYPE) ) {
      response.headers().add(HttpHeaders.Names.CONTENT_TYPE, request.headers().get(HttpHeaders.Names.CONTENT_TYPE))
    }

    response
  }

}
