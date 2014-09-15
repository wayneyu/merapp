package controllers

import scala.concurrent.ExecutionContext.Implicits._
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.mvc._

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Math Exam/Education Resources"))
  }

  def team = Action{
    Ok(views.html.team("About Us"))
  }

  def question(q: String) = Action.async{

    val ubcwiki = WS.url("http://wiki.ubc.ca/Science:Math_Exam_Resources/" + q).get()

    ubcwiki.map( response => Status(response.status)(response.body).as("text/html"))

  }



}