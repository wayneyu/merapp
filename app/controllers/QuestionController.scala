package controllers

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import reactivemongo.bson.{BSONArray, BSONString, BSONDocument}
import views.html.helper.form
import scala.concurrent.Future
import scala.util.{Success, Failure}

// Reactive Mongo imports
import reactivemongo.api._
import reactivemongo.core.commands._

// Reactive Mongo plugin, including the JSON-specialized collection
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection

/**
 * Created by wayneyu on 11/1/14.
 */
object QuestionController extends Controller with MongoController {

  val collection: JSONCollection = db.collection[JSONCollection]("questions")


  def questions = Action.async {
    val coursesResult = distinctCourses()
    val yearsResult = distinctYears()

    val courseAndYearResult = for {
      cr <- coursesResult
      yr <- yearsResult
    } yield (cr, yr)

    coursesResult onComplete {
      case Success(res) => Logger.debug("No. of courses found: " + res.length.toString())
      case Failure(exception) =>
    }
    yearsResult onComplete {
      case Success(res) => Logger.debug("No. of years found: " + res.length.toString())
      case Failure(exception) =>
    }

    courseAndYearResult.map { case (courseList, yearList) =>
      Ok(views.html.question("", "", Nil, Nil)(courseList, yearList, Nil, "", "", "", ""))
    }

  }

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
        (j \ "sols").as[List[String]])(Nil, Nil, Nil, "", "", "", ""))
      case Nil => {
        val resp = "Question not found"
        Ok(views.html.question(resp,"",Nil,Nil)(Nil, Nil, Nil, "", "", "", ""))
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

  def findByYear(year: Int) = Action.async {
    distinctCourses(year).map( list => Ok(views.html.question("", "", Nil, Nil)(list, Nil, Nil, "", year.toString(), "", "")) )
  }

  def findByCourse(course: String) = Action.async {
    val res = distinctYears(course)
    res onComplete {
      case Success(res) => Logger.debug("No. of years found: " + res.length.toString())
      case Failure(exception) =>
    }
    res.map(list => Ok(views.html.question("", "", Nil, Nil)(Nil, list, Nil, course, "", "", "")))
  }

  def distinctYears(course: String): Future[List[String]] = {
    // set up query
    //{ distinct: "<collection>", key: "<field>", query: <query> }
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "year", "query" -> BSONDocument( "course" -> course)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[List[Int]]("values")
    ).map {
      case Some(list) => list.sorted.map(_.toString())
      case None => Nil
    }

  }

  def distinctCourses(year: Int): Future[List[String]] = {
    // set up query
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "course", "query" -> BSONDocument( "year"-> year ) ))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[List[String]]("values")
    ).map {
      case Some(list) => list.sorted
      case None => Nil
    }
  }

  def distinctYears(): Future[List[String]] = {
    // set up query
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "year"))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[List[Int]]("values")
    ).map {
      case Some(list) => list.sorted.map(_.toString())
      case None => Nil
    }
  }

  def distinctCourses(): Future[List[String]] = {
    // set up query
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "course"))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[List[String]]("values")
    ).map {
      case Some(list) => list.sorted
      case None => Nil
    }
  }

}