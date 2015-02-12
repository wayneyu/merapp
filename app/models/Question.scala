package models

/**
 * Created by wayneyu on 12/11/14.
 */

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads}
import play.modules.reactivemongo.json.BSONFormats.BSONArrayFormat
import reactivemongo.bson._

case class Question ( course: String,
                     year: Int,
                     term: String,
                     question: String,
                     statement: String,
                     hints: List[String],
                     sols: List[String],
                     answer: String,
                     topics: List[String],
                     solvers: List[String],
                     rating: Int,
                     num_votes: Int,
                     flags: List[String],
                     contributors: List[String]){

  def url: String = controllers.routes.QuestionController.question(course, term + "_" + year, question).url

  def link: String = course + " - " + term + " " + year + " - " + question
}


object Question {
  // Generates Writes and Reads for Feed and User thanks to Json Macros
//  implicit val questionWrite = Json.format[Question]

  implicit val QuestionReads: Reads[Question] = (
      (JsPath \ "course").read[String] and
        (JsPath \ "year").read[Int] and
        (JsPath \ "term").read[String] and
        (JsPath \ "question").read[String] and
        (JsPath \ "statement_html").read[String] and
        (JsPath \ "hints_html").read[List[String]] and
        (JsPath \ "sols_html").read[List[String]] and
        (JsPath \ "answer_html").read[String] and
        (JsPath \ "topics").read[List[String]] and
        (JsPath \ "solvers").read[List[String]] and
        (JsPath \ "rating").read[Int] and
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
        doc.getAs[String]("question").get,
        doc.getAs[String]("statement_html").get,
        doc.getAs[List[String]]("hints_html").get,
        doc.getAs[List[String]]("sols_html").get,
        doc.getAs[String]("answer_html").get,
        doc.getAs[List[String]]("topics").getOrElse(Nil),
        doc.getAs[List[String]]("solvers").getOrElse(Nil),
        doc.getAs[Int]("rating").getOrElse(-1),
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
			"question" -> BSONString(q.question),
			"statement_html" -> BSONString(q.statement),
			"hints_html" -> BSONArray(q.topics.map{ BSONString(_) }),
			"sols_html" -> BSONArray(q.topics.map{ BSONString(_) }),
			"answer_html" -> BSONString(q.answer),
			"topics" -> BSONArray(q.topics.map{ BSONString(_) }),
			"solvers" -> BSONArray(q.topics.map{ BSONString(_) }),
			"rating" -> BSONInteger(q.rating),
			"num_votes" -> BSONInteger(q.num_votes),
			"flags" -> BSONArray(q.topics.map{ BSONString(_) }),
			"contributors" -> BSONArray(q.topics.map{ BSONString(_) })
		)
	}

  val empty = Question("",-1,"","No Question","",Nil,Nil,"",Nil,Nil, -1, -1, Nil, Nil)

	def qid(course: String, term_year: String, q: String) = (course + "+" + term_year + "+" + q).replace(" ", "\\s")

	def parseQid(qid: String) = {
		val pattern = """(.*)\+(.*)_(\d\d\d\d)\+(.*)""".r
		qid.replace("\\s", " ") match {
			case pattern(course, term, year, q) => (course, term, year.toInt, q)
		}
	}
}

case class SearchResult (course: String,
                      year: Int,
                      term: String,
                      question: String,
                      statement: String,
                      textScore: Double) {

  def url: String = controllers.routes.QuestionController.question(course, term + "_" + year, question).url

  def link: String = course + " - " + term + " " + year + " - " + question

  def score = BigDecimal(textScore).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
}

object SearchResult {

  implicit object SearchResultReader extends BSONDocumentReader[SearchResult] {
    def read(doc: BSONDocument): SearchResult = {
      SearchResult(
        doc.getAs[String]("course").get,
        doc.getAs[Int]("year").get,
        doc.getAs[String]("term").get,
        doc.getAs[String]("question").get,
        doc.getAs[String]("statement_html").get,
        doc.getAs[Double]("textScore").get
      )
    }
  }

  val empty = SearchResult("", -1, "", "", "", 0)

}


sealed trait Term
case object December extends Term
case object April extends Term
