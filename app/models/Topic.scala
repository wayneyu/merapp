package models

import reactivemongo.bson.{BSONDocument, BSONDocumentReader}

/**
 * Created by wayneyu on 1/8/15.
 */
case class Topic(contents: List[TopicContent],
                 parent: String,
                 topic: String,
                 url: String) {

}

object Topic {

  implicit object TopicReader extends BSONDocumentReader[Topic] {
    def read(doc: BSONDocument): Topic = {
      Topic(
        doc.getAs[List[TopicContent]]("content").getOrElse(Nil),
        doc.getAs[String]("parent").getOrElse(""),
        doc.getAs[String]("topic").getOrElse(""),
        doc.getAs[String]("url").getOrElse("")
      )
    }
  }

  val empty = Topic(Nil, "", "", "")

}