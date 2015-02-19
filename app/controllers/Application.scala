package controllers

import play.Routes
import play.api.cache.Cached
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

  def index = Cached(routes.Application.index().url) {
	  UserAwaredAction.async { implicit context =>
		  Future(Ok(views.html.index()))
	  }
  }

  def team = Cached(routes.Application.team().url) {
	  UserAwaredAction.async { implicit context =>
		  Future(Ok(views.html.team()))
	  }
  }

  def questions = Cached(routes.Application.questions().url) {
	  QuestionController.questions()
  }

  def editor = UserAwaredAction.async { implicit context =>
    Future(Ok(views.html.editor()))
  }

  def search = UserAwaredAction.async { implicit context =>
    Future(Ok(views.html.search(List())))
  }

  def exams = Cached(routes.Application.exams().url) {
	  QuestionController.exams()
  }

  def topics = Cached(routes.Application.topics().url) {
	  TopicController.topics()
  }

  def javascriptRoutes = Action { implicit request =>
    Logger.debug("javascriptRoutes: ")
    Ok(
      Routes.javascriptRouter("jsRoutes")
    ).as("text/javascript")
  }
}
