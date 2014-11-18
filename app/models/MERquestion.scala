package models

import reactivemongo.bson.{BSONDocumentReader, BSONDocument, BSONObjectID}

case class Question(
                 course: String,
                 year: Int,
                 term: String,
                 question: String,
                 statement: String,
                 hints: List[String],
                 sols: List[String])


object JsonFormats {
  import play.api.libs.json.Json
  import play.api.data._
  import play.api.data.Forms._

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val qFormat = Json.format[Question]
}

object Question {
  implicit object PersonReader extends BSONDocumentReader[Question] {
    def read(doc: BSONDocument): Question = {
//      val id = doc.getAs[BSONObjectID]("_id").get
      val course = doc.getAs[String]("course").get
      val year = doc.getAs[Int]("year").get
      val term = doc.getAs[String]("term").get
      val question = doc.getAs[String]("question").get
      val statement = doc.getAs[String]("statement").get
      val hints = doc.getAs[List[String]]("hints").get
      val sols = doc.getAs[List[String]]("sols").get

      Question(course, year, term, question, statement, hints, sols)
    }
  }
}