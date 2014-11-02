package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
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
object DBController extends Controller with MongoController {

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
        (j \ "statement").as[String], q,
        (j \ "hints").as[List[String]],
        (j \ "sols").as[List[String]]))
      case Nil => BadRequest("Question not found")
    }

  }
}
