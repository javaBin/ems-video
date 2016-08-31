package ems

import argonaut._, Argonaut._
import java.net.URI

case class Property(name: String, value: Json)

object Property {
  implicit val propertyCodec: CodecJson[Property] = CodecJson(
    p => Json.obj(
      "name" := p.name,
      "value" := p.value
    ),
    c => for {
      name <- (c --\ "name").as[String]
      value <- (c --\ "value").as[Json] ||| (c --\ "array").as[Json] ||| (c --\ "object").as[Json]
    } yield {
      Property(name, value)
    }
  )
}


case class Link(href: URI, rel: String)

object Link {
  import StandardCodecs._
  implicit val linkCodec: CodecJson[Link] = casecodec2(Link.apply, Link.unapply)("href", "rel")
}

case class Item(href: URI, properties: Vector[Property], links: Vector[Link])

object Item {
  import StandardCodecs._
  implicit val itemCodec: CodecJson[Item] = casecodec3(Item.apply, Item.unapply)("href", "data", "links")
}


case class Collection(href: URI, items: Vector[Item], links: Option[Vector[Link]])

object Collection {
  import StandardCodecs._

  val collectionCodecInternal = casecodec3(Collection.apply, Collection.unapply)("href", "items", "links")
  implicit val collectionCodec: CodecJson[Collection] = CodecJson(
    c => Json.obj(
      "collection" := Json.obj(
        "version" := "1.0"
      ).deepmerge(collectionCodecInternal.encode(c))
    ),
    hc => for {
      coll <- (hc --\ "collection").as[Collection](collectionCodecInternal)
    } yield coll
  )
}


object StandardCodecs {
  implicit def uriCodec: CodecJson[URI] = CodecJson.derived[String].xmap(URI.create)(_.toString)
}
