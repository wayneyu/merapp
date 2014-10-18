package controllers

import play.api.Play
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
    val file = Play.resource("public/raw_database/json_data/" + course + "/" + year + "/" + q + ".json")
    file match {
      case Some(f) =>
        val js = Json.parse(Source.fromFile(f.toURI).getLines().mkString)
        val stmt = (js \ "statement").as[String]
        val hints = (js \ "hints").as[List[String]]
        val sols = (js \ "sols").as[List[String]]
        Ok(views.html.question(stmt, q, hints, sols))
      case None =>
        BadRequest("File not found")
    }

  }



}