package controllers

import play.Routes
import play.api.{Logger, Play}
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

  def questions = QuestionController.questions()

  def editor = Action{
    Ok(views.html.editor())
  }

  def javascriptRoutes = Action { implicit request =>
    Logger.info("javascriptRoutes: ")
    Ok(
      Routes.javascriptRouter("jsRoutes",
        routes.javascript.QuestionController.findByCourse
      )
    ).as("text/javascript")
  }

}