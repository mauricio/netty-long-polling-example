package example

import java.util.Date
import io.netty.channel.ChannelHandlerContext

case class ClientKey(path: String, expiration: Date, ctx : ChannelHandlerContext)
  extends Comparable[ClientKey] {

  override def compareTo(o: ClientKey): Int =
    expiration.compareTo(o.expiration)

  def isExpired : Boolean = new Date().after(expiration)


}
