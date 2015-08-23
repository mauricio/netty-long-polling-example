package example

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object FutureUtils {

  def awaitFuture[T](future: Future[T]): T =
    Await.result(future, 5 seconds)

}
