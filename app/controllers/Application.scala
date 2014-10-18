package controllers

import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._

import scala.io.Source

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Math Exam/Education Resources"))
  }

  def team = Action{
    Ok(views.html.team("About Us"))
  }

  def question(course: String, year: String, q: String) = Action {
    val file = "/home/wayneyu/mer/raw_database/json_data/" + course + "/" + year + "/" + q + ".json"
    //val file = Assets.at("/public/raw_database/json_data/" + course + "/" + year + "/", q + ".json");
    val js = Json.parse(Source.fromFile(file).getLines().mkString)
    val stmt = (js \ "statement").as[String]
    val hints: List[String] = (js \ "hints" ).as[List[String]]
    val sols: List[String] = (js \ "sols").as[List[String]]
//    val hint = hints(0) match {
//      case Some(str) => str
//      case None => ""
//    }
//    val sol = sols(0) match {
//      case Some(str) => str
//      case None => ""
//    }
    Ok(views.html.question(stmt, q, hints(0), sols(0)))
  }



}