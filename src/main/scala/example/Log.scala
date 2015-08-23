package example

import org.slf4j.LoggerFactory

object Log {

  def get[T](implicit tag: reflect.ClassTag[T]) =
    LoggerFactory.getLogger(tag.runtimeClass.getName)

  def getByName(name: String) =
    LoggerFactory.getLogger(name)

}
