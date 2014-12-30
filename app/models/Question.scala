package models

/**
 * Created by wayneyu on 12/11/14.
 */

import java.net.URL

import controllers.{Application, QuestionController}
import reactivemongo.bson.{BSONDocumentReader, BSONDocument, BSONObjectID}
import play.api.libs.json.{JsPath, Reads, Json}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.functional.syntax._
import views.html.question


case class Question ( course: String,
                     year: Int,
                     term: String,
                     question: String,
                     statement: String,
                     hints: List[String],
                     sols: List[String],
                     topics: List[String],
                     solvers: List[String],
                     rating: Int,
                     num_votes: Int,
                     flags: List[String],
                     contributors: List[String]){

  def url: String = controllers.routes.QuestionController.question(course, term + "_" + year, question).url

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
        doc.getAs[List[String]]("topics").getOrElse(Nil),
        doc.getAs[List[String]]("solvers").get,
        doc.getAs[Int]("rating").get,
        doc.getAs[Int]("num_votes").get,
        doc.getAs[List[String]]("flags").get,
        doc.getAs[List[String]]("contributors").get
      )
    }
  }

  val empty = Question("",-1,"","No Question","",Nil,Nil,Nil,Nil, -1, -1, Nil, Nil)

//  def empty(course: String, year: String, term: String) =
//    Question(course, year.toInt, term, "No Question","",Nil,Nil)
}

sealed trait Term
case object December extends Term
case object April extends Term
