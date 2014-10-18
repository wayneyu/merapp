package controllers

import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._

import scala.io.Source

/**
 * Created by wayneyu on 10/17/14.
 */
object QuestionJsonController extends Controller{


//  def question(course: String, year: String, q: String) = Action {
//    val file = "/home/wayneyu/mer/raw_database/json_data/" + course + "/" + "December_2013" + "/" + "Question_01_(a)" + ".json"
//    val js = Json.parse(Source.fromFile(file).getLines().mkString)
//    val stmt = (js \ "statement").as[String]
//    val hints = (js \ "hints").as[String]
//    val soln = (js \ "sols").as[String]
//    Ok(views.html.question(stmt))
//  }

}

//case class question( term: String, course: String, stmt: String, year: Int, sols: String, hints: String)