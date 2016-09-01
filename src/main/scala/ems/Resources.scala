package ems

import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

import unfiltered.filter._
import unfiltered.filter.request._
import unfiltered.request._
import unfiltered.response._
import dispatch._
import Defaults._
import argonaut._
import Argonaut._
import org.codehaus.httpcache4j.uri.URIBuilder

import scala.concurrent.Await
import scala.concurrent.duration._

case class Resources(baseURI:String, eventRoot: URI, redirect: URI, credentials: Option[Credentials]) extends Plan {
  private val cache = scala.collection.concurrent.TrieMap[URI, Collection]()
  private val client = Http.configure(_.setFollowRedirects(true))

  val intent: Plan.Intent = {
    case ContextPath(_, Seg(Nil)) => {
      Ok ~> Html5(Snippets.page(baseURI,
        <div>Choose events from menu</div>
      ))
    }

    case ContextPath(_, p@Seg("events" :: Nil)) => {
      val allEvents = loadEvents(eventRoot)
      Ok ~> Html5(Snippets.page(baseURI,
        <ul>
          {allEvents.map(e => <li> <a href={s"$baseURI$p/${e.slug}/sessions"} >{e.name}</a></li>)}
        </ul>
      ))
    }
    case ContextPath(_, p@Seg("events" :: slug :: "sessions" :: Nil)) => {
      val event = loadEvents(eventRoot).find(_.slug == slug)
      val allSessions = event.map(e => loadSessions(e.sessions)).getOrElse(Vector.empty)
      Ok ~> Html5(Snippets.page(baseURI,
        <ul>
          {allSessions.map(s => <li> <a href={s"$baseURI$p/${s.slug}"}>{s.title}</a></li>)}
        </ul>
      ))
    }
    case req@ContextPath(_, Seg("events" :: eventSlug :: "sessions" :: slug :: Nil)) => {
      val uri = URIBuilder.fromURI(redirect).addParameter("event-slug", eventSlug).addParameter("session-slug", slug).toURI
      val res = Await.result(load(uri), 10.seconds)
      val session = res.flatMap(cl => toSessions(cl.collection).headOption)

      req match {
        case POST(_) & Params(p) => {
          val postdata = for {
            video <- p("video").headOption
            s <- session
          } yield {
            (s.href, video)
          }

          def authOrNot(req: Req): Req = credentials.map(c => req as_!(c.username, c.password)).getOrElse(req)

          postdata.map{case (href, vimeo) =>
              Await.result(
                client(
                  authOrNot(url(href.toString) <:< Map("If-Unmodified-Since" -> res.flatMap(_.lastModified).map(DateTimeFormatter.RFC_1123_DATE_TIME.format(_)).getOrElse("*")) << Map("video" -> vimeo))
                ).map(res => Status(res.getStatusCode)),
                10.seconds)
          }.getOrElse(NotFound)
        }
        case GET(_) => {
          session.map{ s =>
            Ok ~> Html5(
              Snippets.page(baseURI,
                <div class="row">
                  <h1>{s.title}</h1>
                </div>
                  <div class="row">Last updated { res.get.lastModified.map(_.toString).getOrElse("Never") }</div>
                  <div class="row">{s.video.map(v => <a href={v.toString}>Registered Video</a>).getOrElse("No video is registered")}</div>
                  <div class="row">
                    <form method="post">
                      <label><span>Vimeo url</span>
                        <input name="video" type="url"></input>
                      </label>
                      <input type="submit" class="button button-big"></input>
                    </form>
                  </div>
              ))
          }.getOrElse(NotFound ~> ResponseString("Not found"))

        }
      }
    }
  }

  def toSessions(c: Collection) = {
    def toSession(i: Item): Option[Session] = for {
      title <- i.properties.find(_.name == "title")
      slug <- i.properties.find(_.name == "slug")
    } yield Session(i.href, title.value.stringOrEmpty, slug.value.stringOrEmpty, i.links.find(_.rel == "alternate video").map(_.href))

    c.items.flatMap(toSession)
  }

  def loadSessions(href: URI): Vector[Session] = {
    loadCollection(href).map(toSessions).getOrElse(Vector.empty)
  }

  def loadEvents(href: URI): Vector[Event] = {
    def toEvent(i: Item): Option[Event] = for {
      name <- i.properties.find(_.name == "name")
      slug <- i.properties.find(_.name == "slug")
      link <- i.links.find(_.rel == "session collection")
    } yield Event(i.href, name.value.stringOrEmpty, slug.value.stringOrEmpty, link.href)

    loadCollection(href).map(_.items.flatMap(toEvent)).getOrElse(Vector.empty)
  }


  def loadCollection(href: URI): Option[Collection] = {
    def load(): Option[Collection] = Await.result(
      client(url(href.toString)).map(_.getResponseBody("utf-8").decodeEither[Collection].fold(
        x => {
          println(x)
          None
        },
        Some(_)
      )), 10.seconds)

    cache.get(href).orElse{
      val c = load()
      c.foreach(c => cache.put(href, c))
      c
    }
  }

  def load(href: URI): Future[Option[CollectionWithLastModified]] = {
    client(url(href.toString)).map{res =>
      val lm = Option(res.getHeader("Last-Modified")).map(OffsetDateTime.parse(_, DateTimeFormatter.RFC_1123_DATE_TIME))
      res.getResponseBody("utf-8").decodeOption[Collection].map(c =>
        CollectionWithLastModified(c, lm)
      )
    }
  }
}

case class CollectionWithLastModified(collection: Collection, lastModified: Option[OffsetDateTime])

case class Session(href: URI, title: String, slug: String, video: Option[URI])

case class Event(href: URI, name: String, slug:String, sessions: URI)
