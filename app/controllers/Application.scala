package controllers

import play.Routes
import play.api.{Logger, Play}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._

import scala.concurrent.Future
import scala.io.Source

object Application extends Controller {

  def index = Action.async {
    Future(Ok(views.html.index("Math Exam/Education Resources")))
  }

  def team = Action.async {
    Future(Ok(views.html.team("About Us")))
  }

  def questions = QuestionController.questions()

  def editor = Action.async {
    Future(Ok(views.html.editor()))
  }

  def search = Action.async {
    Future(Ok(views.html.search(List())))
  }

  def exams = QuestionController.exams()

  def topics = TopicController.topics()

  def javascriptRoutes = Action { implicit request =>
    Logger.info("javascriptRoutes: ")
    Ok(
      Routes.javascriptRouter("jsRoutes",
        routes.javascript.QuestionController.distinctYears,
        routes.javascript.QuestionController.distinctCourses
      )
    ).as("text/javascript")
  }

}