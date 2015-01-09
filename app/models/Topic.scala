package models

import java.net.URL

import play.api.libs.json.{JsPath, Reads}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}
import play.api.libs.functional.syntax._

/**
 * Created by wayneyu on 1/8/15.
 */
case class Topic(contents: List[TopicContent],
                 parent: String,
                 topic: String,
                 url: String) {

}

object Topic {
  implicit val TopicReads: Reads[Topic] = (
    (JsPath \ "content").read[List[TopicContent]] and
      (JsPath \ "parent").read[String] and
      (JsPath \ "topic").read[String] and
      (JsPath \ "url").read[String]
    )(Topic.apply _)

  implicit object TopicReader extends BSONDocumentReader[Topic] {
    def read(doc: BSONDocument): Topic = {
      Topic(
        doc.getAs[List[TopicContent]]("content").getOrElse(Nil),
        doc.getAs[String]("parent").get,
        doc.getAs[String]("topic").get,
        doc.getAs[String]("url").get
      )
    }
  }

  val empty = Topic(Nil, "", "", "")

}