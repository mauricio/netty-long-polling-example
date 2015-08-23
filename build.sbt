name := "netty-long-polling-example"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.12"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

libraryDependencies += "io.netty" % "netty-all" % "4.0.30.Final"

libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test

libraryDependencies += "org.specs2" %% "specs2-core" % "3.6.3" % Test

libraryDependencies += "org.specs2" %% "specs2-mock" % "3.6.3" % Test

libraryDependencies += "com.ning" % "async-http-client" % "1.9.30" % Test
