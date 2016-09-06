package ems

import scala.util.Properties
import unfiltered.jetty
import java.net.URI
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.codehaus.httpcache4j.uri.URIBuilder
import unfiltered.filter.{Plan, Planify}
import unfiltered.kit.Auth

object Jetty {
  def main(args: Array[String]): Unit = {
    val port = Properties.propOrElse("PORT", Properties.envOrElse("PORT", "8082")).toInt
    val baseURI = Properties.propOrElse("baseURI", Properties.envOrElse("baseURI", s"http://localhost:$port"))
    val emsBase = URI.create(Properties.propOrElse("emsBase", Properties.envOrElse("emsBase", "http://localhost:8081/server/")))
    val emsEvents = URIBuilder.fromURI(emsBase).addPath("events").toURI
    val redirectURI = URIBuilder.fromURI(emsBase).addPath("redirect").toURI
    val emsCredentials = for {
      username <- Properties.propOrNone("emsUsername").orElse(Properties.envOrNone("emsUsername"))
      password <- Properties.propOrNone("emsPassword").orElse(Properties.envOrNone("emsPassword"))
    } yield Credentials(username, password)


    val appCredentials = for {
      username <- Properties.propOrNone("appUsername").orElse(Properties.envOrNone("appUsername"))
      password <- Properties.propOrNone("appPassword").orElse(Properties.envOrNone("appPassword"))
    } yield Credentials(username, password)

    println(s"""
      |Starting App using port $port and
      |BaseURI is $baseURI
      |Using EMS from $emsBase
      |
      |Using EMS credentials ${emsCredentials.map(_.username).getOrElse("No Credentials")}
      |Using app credentials ${appCredentials.map(_.username).getOrElse("No Credentials")}
    """.stripMargin)

    val authBasic: (Plan.Intent => Plan.Intent) = Auth.basic[HttpServletRequest, HttpServletResponse](
      (u, p) => appCredentials.exists(c => c.username == u && c.password == p)
    )(_: Plan.Intent, Auth.defaultFail("emsVideo"))

    val http = jetty.Server.http(port)

    http.plan(
      Planify(authBasic(Resources(baseURI, emsEvents, redirectURI, emsCredentials).intent))
    ).run()
    ()
  }
}
