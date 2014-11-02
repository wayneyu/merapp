package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import views.html.helper.form
import scala.concurrent.Future
import scala.util.{Success, Failure}

// Reactive Mongo imports
import reactivemongo.api._

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

/**
 * Created by wayneyu on 11/1/14.
 */
object QuestionController extends Controller with MongoController {

  def collection: JSONCollection = db.collection[JSONCollection]("questions")

  def question(course: String, term_year: String, q: String) = Action.async {

    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt

    // let's do our query
    val cursor: Cursor[JsObject] = collection.
      // find all people with name `name`
      find(Json.obj("course" -> course, "term" -> term, "year" -> year, "question" -> q)).
      // perform the query and get a cursor of JsObject
      cursor[JsObject]

    // gather all the JsObjects in a list
    val futureQuestion: Future[List[JsObject]] = cursor.collect[List]()

    // log some info
    futureQuestion onComplete {
      case Success(res) => Logger.debug("No. of questions found: " + res.length.toString())
      case Failure(exception) =>
    }

    futureQuestion.map {
      case j::js => Ok(views.html.question(
        q, (j \ "statement").as[String],
        (j \ "hints").as[List[String]],
        (j \ "sols").as[List[String]]))
      case Nil => {
        val resp = "Question not found"
        Ok(views.html.question(resp,"",Nil,Nil))
      }
    }
  }

  def questionSubmit() = Action { request =>
    val form = request.body.asFormUrlEncoded
    form match {
      case Some(map) => {
        val q_no = map.getOrElse("q_no", Seq())(0)
        val q_letter = map.getOrElse("q_letter", Seq())(0)
        Redirect(controllers.routes.QuestionController.question(
          map.getOrElse("course", Seq())(0),
          map.getOrElse("term", Seq())(0) + "_" + map.getOrElse("year", Seq())(0),
          if (q_no.nonEmpty && q_letter.nonEmpty) "Question_" + q_no + "_(" + q_letter + ")" else ""))
      }
      case None => Redirect(controllers.routes.QuestionController.question("","",""))
    }
  }

}
