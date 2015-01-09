package models

import play.api.libs.json.{JsPath, Reads}
import reactivemongo.bson.{BSONDocument, BSONDocumentReader}
import play.api.libs.functional.syntax._

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

  val empty = TopicContent("", "", "", "")

}