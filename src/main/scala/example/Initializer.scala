package example

import java.util.concurrent.TimeUnit

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelFuture, Channel, ChannelOption, ChannelInitializer}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}

import scala.concurrent.ExecutionContext

object Initializer {

  val log = Log.get[Initializer]

}

class Initializer (timeoutInSeconds : Int, val port: Int) (implicit executor: ExecutionContext)
  extends ChannelInitializer[SocketChannel] {

  import Initializer.log

  private val bossGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()

  private val serverBootstrap = new ServerBootstrap()
  serverBootstrap.option(ChannelOption.SO_BACKLOG, java.lang.Integer.valueOf(1024))
  serverBootstrap.group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .childHandler(this)

  private var serverChannel: Channel = null
  private val setHeadersHandler = new SetHeadersHandler
  private val mainHandler = new MainHandler(new ClientsRegistry(timeoutInSeconds))

  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline()

    p.addLast("http-codec", new HttpServerCodec())
    p.addLast("aggregator", new HttpObjectAggregator(Int.MaxValue))
    p.addLast("set-headers-handler", setHeadersHandler)
    p.addLast("handler", mainHandler)
  }

  def start(): Unit = {
    try {
      serverChannel = serverBootstrap.bind(port).sync().channel()
      serverChannel.eventLoop().scheduleAtFixedRate(new Runnable {
        override def run(): Unit =
          mainHandler.evaluateTimeouts()
      },
        timeoutInSeconds,
        timeoutInSeconds,
        TimeUnit.SECONDS
      )

      log.info(s"Starting server ${serverChannel}")
      serverChannel.closeFuture().sync()
    } catch {
      case e: Exception =>
        log.error(s"Server channel failed with ${e.getMessage}", e)
    }
    finally {
      bossGroup.shutdownGracefully()
      workerGroup.shutdownGracefully()
    }
  }

  def stop(): ChannelFuture = {
    log.info(s"Stopping server ${serverChannel}")
    val channelFuture = serverChannel.close().awaitUninterruptibly()
    log.info(s"Closed server channel ${serverChannel}")
    channelFuture
  }

}

