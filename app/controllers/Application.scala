package controllers

import play.Routes
import play.api.{Logger, Play}
import play.api.libs.json.Json
import securesocial.core.{RuntimeEnvironment, SecureSocial}
import service.{ServiceComponent, User}

import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ApplicationController extends securesocial.core.SecureSocial[User] with ServiceComponent{

  override implicit val env = AuthRuntimeEnvironment

  def index = UserAwareAction.async { implicit request =>
	  implicit val user = request.user
    Future(Ok(views.html.index()))
  }

  def team = UserAwareAction.async { implicit request =>
	  implicit val user = request.user
    Future(Ok(views.html.team()))
  }

  def questions = QuestionController.questions()

  def editor = UserAwareAction.async { implicit request =>
	  implicit val user = request.user
    Future(Ok(views.html.editor()))
  }

  def search = UserAwareAction.async { implicit request =>
	  implicit val user = request.user
    Future(Ok(views.html.search(List())))
  }

  def exams = QuestionController.exams()

  def topics = TopicController.topics()

  def javascriptRoutes = Action { implicit request =>
    Logger.info("javascriptRoutes: ")
    Ok(
      Routes.javascriptRouter("jsRoutes")
    ).as("text/javascript")
  }


}

object Application extends ApplicationController