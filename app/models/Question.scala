package models

/**
 * Created by wayneyu on 12/11/14.
 */

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import play.modules.reactivemongo.json.BSONFormats.BSONArrayFormat
import reactivemongo.bson._
import assets._

case class Question(course: String,
                     year: Int,
                     term: String,
                     term_year: String,
                     number: String,
                     number_human: String,
                     question: String, // deprecated, use number_human
                     ID: String,
                     is_multiple_choice: Boolean,
                     multiple_choice_answers: List[String],
                     statement: String,
                     hints: List[String],
                     sols: List[String],
                     answer: String,
                     topics: List[String],
                     solvers: List[String],
                     rating: Double,
                     num_votes: Int,
                     flags: List[String],
                     contributors: List[String])
{
  def url: String = controllers.routes.QuestionController.question(course, term_year, number).url

  def link: String = course + " - " + term + " " + year + " - " + number_human
}


object Question {
// Generates Writes and Reads for Feed and User thanks to Json Macros
//  implicit val questionWrite = Json.format[Question]

  implicit val QuestionReads: Reads[Question] = (
        (JsPath \ "course").read[String] and
        (JsPath \ "year").read[Int] and
        (JsPath \ "term").read[String] and
        (JsPath \ "term_year").read[String] and
        (JsPath \ "number").read[String] and
        (JsPath \ "number_human").read[String] and
        (JsPath \ "question").read[String] and
        (JsPath \ "ID").read[String] and
        (JsPath \ "is_multiple_choice").read[Boolean] and
        (JsPath \ "multiple_choice_answers").read[List[String]] and
        (JsPath \ "statement_html").read[String] and
        (JsPath \ "hints_html").read[List[String]] and
        (JsPath \ "sols_html").read[List[String]] and
        (JsPath \ "answer_html").read[String] and
        (JsPath \ "topics").read[List[String]] and
        (JsPath \ "solvers").read[List[String]] and
        (JsPath \ "rating").read[Double] and
        (JsPath \ "num_votes").read[Int] and
        (JsPath \ "flags").read[List[String]] and
        (JsPath \ "contributors").read[List[String]]
    )(Question.apply _)

  implicit object QuestionReader extends BSONDocumentReader[Question] {
    def read(doc: BSONDocument): Question = {
      Question(
        doc.getAs[String]("course").get,
        doc.getAs[Int]("year").get,
        doc.getAs[String]("term").get,
        doc.getAs[String]("term_year").getOrElse(""),
        doc.getAs[String]("number").getOrElse(""),
        doc.getAs[String]("number_human").getOrElse(""),
        doc.getAs[String]("question").getOrElse(""),
        doc.getAs[String]("ID").getOrElse(""),
        doc.getAs[Boolean]("is_multiple_choice").getOrElse(false),
        doc.getAs[List[String]]("multiple_choice_answers").getOrElse(Nil),
        doc.getAs[String]("statement_html").get,
        doc.getAs[List[String]]("hints_html").get,
        doc.getAs[List[String]]("sols_html").get,
        doc.getAs[String]("answer_html").get,
        doc.getAs[List[String]]("topics").getOrElse(Nil),
        doc.getAs[List[String]]("solvers").getOrElse(Nil),
        doc.getAs[Double]("rating").getOrElse(-1),
        doc.getAs[Int]("num_votes").get,
        doc.getAs[List[String]]("flags").getOrElse(Nil),
        doc.getAs[List[String]]("contributors").getOrElse(Nil)
      )
    }
  }

	implicit object QuestionWriter extends BSONDocumentWriter[Question] {
		def write(q: Question): BSONDocument = BSONDocument(
			"course" -> BSONString(q.course),
			"year" -> BSONInteger(q.year),
			"term" -> BSONString(q.term),
      "term_year" -> BSONString(q.term_year),
      "number" -> BSONString(q.number),
      "number_human" -> BSONString(q.number_human),
      "question" -> BSONString(q.question),
      "ID" -> BSONString(q.ID),
      "is_multiple_choice" -> BSONBoolean(q.is_multiple_choice),
      "multiple_choice_answers" -> BSONArray(q.multiple_choice_answers),
			"statement_html" -> BSONString(q.statement),
			"hints_html" -> BSONArray(q.topics.map{ BSONString(_) }),
			"sols_html" -> BSONArray(q.topics.map{ BSONString(_) }),
			"answer_html" -> BSONString(q.answer),
			"topics" -> BSONArray(q.topics.map{ BSONString(_) }),
			"solvers" -> BSONArray(q.topics.map{ BSONString(_) }),
			"rating" -> BSONDouble(q.rating),
			"num_votes" -> BSONInteger(q.num_votes),
			"flags" -> BSONArray(q.topics.map{ BSONString(_) }),
			"contributors" -> BSONArray(q.topics.map{ BSONString(_) })
		)
	}

  val empty = Question(course = "",
                       year = -1,
                       term = "",
                       term_year = "",
                       number = "",
                       number_human = "",
                       question = "",
                       ID = "",
                       is_multiple_choice = false,
                       multiple_choice_answers = Nil,
                       statement = "No Content Found",
                       hints = Nil,
                       sols = Nil,
                       answer = "" ,
                       topics = Nil,
                       solvers = Nil,
                       rating = -1,
                       num_votes = 0,
                       flags = Nil,
                       contributors = Nil)

  def qid(course: String, term_year: String, q: String) = (course + "+" + term_year + "+" + q).replace(" ", "\\s")

	def parseQid(qid: String) = {
		val pattern = """(.*)\+(.*)\+(.*)_(\d\d\d\d)\+(.*)""".r
		qid match {
			case pattern(school, course, term, year, number) => (course, term, year.toInt, number)
		}
	}
}

case class SearchResult (ID: String,
                      statement: String,
                      textScore: Double) {

  val (course, term_year, number) = assets.course_and_term_year_and_number_from_ID(ID)
  val number_human: String = assets.number_human_from_number(number)

  def url: String = controllers.routes.QuestionController.question(course, term_year, number_human).url

  def link: String = course + " - " + term_year.replace("_", " ") + " - " + number_human

  def score = BigDecimal(textScore).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
}

object SearchResult {

  implicit object SearchResultReader extends BSONDocumentReader[SearchResult] {
    def read(doc: BSONDocument): SearchResult = {
      SearchResult(
        doc.getAs[String]("ID").getOrElse(""),
        doc.getAs[String]("statement_html").getOrElse(""),
        doc.getAs[Double]("textScore").getOrElse(0)
      )
    }
  }

  val empty = SearchResult(ID = "",
                           statement = "",
                           textScore = 0)

}


sealed trait Term
case object December extends Term
case object April extends Term
