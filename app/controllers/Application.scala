package controllers

import play.Routes
import play.api.{Logger, Play}
import play.api.libs.json.Json
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import service._

import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Application extends ServiceComponent {

  def index = UserAwaredAction.async { implicit context =>
    Future(Ok(views.html.index()))
  }

  def team = UserAwaredAction.async { implicit context =>
    Future(Ok(views.html.team()))
  }

  def questions = QuestionController.questions()

  def editor = UserAwaredAction.async { implicit context =>
    Future(Ok(views.html.editor()))
  }

  def search = UserAwaredAction.async { implicit context =>
    Future(Ok(views.html.search(List())))
  }

  def exams = QuestionController.exams()

  def topics = TopicController.topics()

  def javascriptRoutes = Action { implicit request =>
    Logger.debug("javascriptRoutes: ")
    Ok(
      Routes.javascriptRouter("jsRoutes")
    ).as("text/javascript")
  }


}
