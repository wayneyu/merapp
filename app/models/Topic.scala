package models

import reactivemongo.bson._

/**
 * Created by wayneyu on 1/8/15.
 */
case class Topic(contents: List[TopicContent] = Nil,
                 parent: String = "",
                 topic: String,
                 url: String = "")

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

	implicit object TopicWriter extends BSONDocumentWriter[Topic] {
		def write(t: Topic): BSONDocument = {
			BSONDocument(
				"content" -> BSONArray(t.contents.map{BSON.write(_)}.toList),
				"parent" -> BSONString(t.parent),
				"topic" -> BSONString(t.topic),
				"url" -> BSONString(t.url)
			)
		}
	}

  val empty = Topic(Nil, "", "", "")

}