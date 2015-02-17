package models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import reactivemongo.bson.{BSONString, BSONDocumentWriter, BSONDocument, BSONDocumentReader}

/**
 * Created by wayneyu on 1/8/15.
 */
case class TopicContent(credit: String,
                        src: String,
                        text: String,
                        title: String){


}

object TopicContent {
  implicit val TopicContentReads: Reads[TopicContent] = (
    (JsPath \ "credit").read[String] and
      (JsPath \ "src").read[String] and
      (JsPath \ "text").read[String] and
      (JsPath \ "title").read[String]
    )(TopicContent.apply _)

  implicit object TopicContentReader extends BSONDocumentReader[TopicContent] {
    def read(doc: BSONDocument): TopicContent = {
      TopicContent(
        doc.getAs[String]("credit").get,
        doc.getAs[String]("src").get.replace("watch?=","embed/"),
        doc.getAs[String]("text").get,
        doc.getAs[String]("title").get
      )
    }
  }

	implicit object TopicContentWriter extends BSONDocumentWriter[TopicContent] {
		def write(c: TopicContent): BSONDocument = {
			BSONDocument(
				"credit" -> BSONString(c.credit),
				"src" -> BSONString(c.src),
				"text" -> BSONString(c.text),
				"title" -> BSONString(c.title)
			)
		}
	}

  val empty = TopicContent("", "", "", "")

}