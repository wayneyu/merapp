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
                     sols: List[String]){

  def url: String = controllers.routes.QuestionController.question(course, term + "_" + year, question).url

}


object Question {
//  implicit object QuestionReader extends BSONDocumentReader[Question] {
//    def read(doc: BSONDocument): Question = {
//      //      val id = doc.getAs[BSONObjectID]("_id").get
//      val course = doc.getAs[String]("course").get
//      val year = doc.getAs[Int]("year").get
//      val term = doc.getAs[String]("term").get
//      val question = doc.getAs[String]("question").get
//      val statement = doc.getAs[String]("statement").get
//      val hints = doc.getAs[List[String]]("hints").get
//      val sols = doc.getAs[List[String]]("sols").get
//
//      Question(course, year, term, question, statement, hints, sols)
//    }
//  }
  // Generates Writes and Reads for Feed and User thanks to Json Macros
//  implicit val questionWrite = Json.format[Question]

  implicit val QuestionReads: Reads[Question] = (
      (JsPath \ "course").read[String] and
        (JsPath \ "year").read[Int] and
        (JsPath \ "term").read[String] and
        (JsPath \ "question").read[String] and
        (JsPath \ "statement").read[String] and
        (JsPath \ "hints").read[List[String]] and
        (JsPath \ "sols").read[List[String]]
    )(Question.apply _)

  implicit object QuestionReader extends BSONDocumentReader[Question] {
    def read(doc: BSONDocument): Question = {
      Question(
        doc.getAs[String]("course").get,
        doc.getAs[Int]("year").get,
        doc.getAs[String]("term").get,
        doc.getAs[String]("question").get,
        doc.getAs[String]("statement").get,
        doc.getAs[List[String]]("hints").get,
        doc.getAs[List[String]]("sols").get
      )
    }
  }

  val empty = Question("",-1,"","No Question","",Nil,Nil)

}
