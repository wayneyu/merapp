package controllers

import play.api._
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Math Exam/Education Resources"))
  }

  def team = Action{
    Ok(views.html.team("About Us"))
  }

}