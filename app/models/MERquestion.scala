package models

case class Question(
                 course: String,
                 year: Int,
                 term: String,
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