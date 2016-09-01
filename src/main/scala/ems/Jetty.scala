package ems

import scala.util.Properties
import unfiltered.jetty
import java.net.URI

import org.codehaus.httpcache4j.uri.URIBuilder

object Jetty {
  def main(args: Array[String]): Unit = {
    val port = Properties.propOrElse("PORT", Properties.envOrElse("PORT", "8082")).toInt
    val contextPath = Properties.propOrElse("contextPath", Properties.envOrElse("contextPath", "/ems-video"))
    val baseURI = Properties.propOrElse("baseURI", Properties.envOrElse("baseURI", s"http://localhost:$port$contextPath"))
    val emsBase = URI.create(Properties.propOrElse("emsBase", Properties.envOrElse("emsBase", "http://localhost:8081/server/")))
    val emsEvents = URIBuilder.fromURI(emsBase).addPath("events").toURI
    val redirectURI = URIBuilder.fromURI(emsBase).addPath("redirect").toURI
    val credentials = for {
      username <- Properties.propOrNone("emsUsername").orElse(Properties.envOrNone("emsUsername"))
      password <- Properties.propOrNone("emsPassword").orElse(Properties.envOrNone("emsPassword"))
    } yield Credentials(username, password)

    println(s"""
      |Starting App using port $port and
      |contextPath=$contextPath
      |BaseURI is $baseURI
      |Using EMS from $emsBase
      |
      |Using credentials ${credentials.map(_.username).getOrElse("No Credentials")}
    """.stripMargin)

    val http = jetty.Server.http(port)
    (if (contextPath == "/" || contextPath.isEmpty) {
      http.plan(Resources(baseURI, emsEvents, redirectURI, credentials))
    } else {
      http.context(contextPath) {
        _.plan(Resources(baseURI, emsEvents, redirectURI, credentials))
      }
    }).run()


    ()
  }
}
