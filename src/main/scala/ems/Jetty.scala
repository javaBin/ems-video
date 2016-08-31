package ems

import scala.util.Properties
import unfiltered.jetty
import java.net.URI

object Jetty {
  def main(args: Array[String]): Unit = {
    val port = Properties.propOrElse("PORT", Properties.envOrElse("PORT", "8082")).toInt
    val contextPath = Properties.propOrElse("contextPath", Properties.envOrElse("contextPath", "/ems-video"))
    val baseURI = Properties.propOrElse("baseURI", Properties.envOrElse("baseURI", s"http://localhost:$port$contextPath"))
    val emsRoot = URI.create(Properties.propOrElse("eventRoot", Properties.envOrElse("eventRoot", "http://localhost:8081/server/events/")))
    val credentials = for {
      username <- Properties.propOrNone("emsUsername").orElse(Properties.envOrNone("emsUsername"))
      password <- Properties.propOrNone("emsPassword").orElse(Properties.envOrNone("emsPassword"))
    } yield Credentials(username, password)

    jetty.Server.http(port).context(contextPath) {
      _.plan(Resources(baseURI, emsRoot, credentials))
    }.run()


    ()
  }
}
