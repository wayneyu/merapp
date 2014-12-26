package controllers

import controllers.Application._
import play.Routes
import play.api._
import play.api.libs.json.{JsArray, Json, JsObject}
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.bson.{BSONValue, BSONArray, BSONDocument}
import play.modules.reactivemongo.json.BSONFormats._
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
      Ok(views.html.question("", "", Nil, Nil)(courseList, Nil, Nil, Nil, "", "", "", ""))
    }
  }

  def question(course: String, term_year: String, q: String) = Action.async {

    val terms = distinctTerms(course) // HELP: Bernhard would like to use this in the "Ok(views.html.question...", but it complains that "String" is expected, but actual is "Anytype".

    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt

    // let's do our query
    val cursor: Cursor[JsObject] = collection.
      find(Json.obj("course" -> course, "term" -> term, "year" -> year, "question" -> q)).
      // perform the query and get a cursor of JsObject
      cursor[JsObject]

    // gather all the JsObjects in a list
    val question: Future[List[JsObject]] = cursor.collect[List]()
    val coursesResult = distinctCourses()
    val yearsResult = distinctYears()

    val res = for {
      cr <- coursesResult
      yr <- yearsResult
      q <- question
    } yield (cr, yr, q)

    res.map { case (courseList, yearList, question) => {
        question match {
          case j :: js =>
            Logger.debug("No. of questions found: " + question.length.toString())
            Ok(views.html.question(
              q, (j \ "statement").as[String],
              (j \ "hints").as[List[String]],
              (j \ "sols").as[List[String]])(courseList, Nil, yearList, Nil, course, year.toString, term, q))
          case Nil => {
            val resp = "Question not found"
            Ok(views.html.question(resp, "", Nil, Nil)(courseList, Nil, Nil, Nil, course, year.toString, term, q))
          }
        }
      }
    }
  }


  def questionSubmit() = Action { request =>
    val form = request.body.asFormUrlEncoded
    form match {
      case Some(map) => {
        for (key <- map.keys){
          Logger.debug(key)
        }
        Redirect(controllers.routes.QuestionController.question(
          map.getOrElse("course", Seq(""))(0),
          map.getOrElse("term", Seq(""))(0) + "_" + map.getOrElse("year", Seq(""))(0),
          map.getOrElse("question", Seq(""))(0)))
      }
      case None => Redirect(controllers.routes.QuestionController.question("","","")  )
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

  def distinctCourses(year: Int, term: String) = Action.async {
    Logger.debug("distinctCourses(year -> " + year + ", term-> " + term + ")")
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "course",
      "query" -> BSONDocument( "year" -> year, "term" -> term)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) =>
        Logger.debug("Result: " + v.toString())
        Ok(BSONArrayFormat.writes(v))
      case None =>
        Logger.debug("Result: no courses found.")
        Ok(Json.obj())
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

  def distinctYears(course: String, term: String) = Action.async {
    Logger.debug("distinctYears(course-> " + course + ", term-> " + term + ")")
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "year",
      "query" -> BSONDocument( "course" -> course, "term" -> term)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) =>
        Logger.debug("Result: " + v.toString())
        Ok(BSONArrayFormat.writes(v))
      case None =>
        Logger.debug("Result: no years found.")
        Ok(Json.obj())
    }
  }

  def distinctTerms(): Future[List[String]] = {
    // set up query
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "term"))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[List[Int]]("values")
    ).map {
      case Some(list) => list.sorted.map(_.toString())
      case None => Nil
    }
  }

  def distinctTerms(course: String) = Action.async {
    // set up query
    //{ distinct: "<collection>", key: "<field>", query: <query> }
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "term", "query" -> BSONDocument( "course" -> course)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) => Ok(BSONArrayFormat.writes(v))
      case None => Ok(Json.obj())
    }
  }

  def distinctTerms(course: String, year: String) = Action.async {
    Logger.debug("distinctYears(course-> " + course + ", year-> " + year + ")")
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "term",
      "query" -> BSONDocument( "course" -> course, "year" -> year)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) =>
        Logger.debug("Result: " + v.toString())
        Ok(BSONArrayFormat.writes(v))
      case None =>
        Logger.debug("Result: no years found.")
        Ok(Json.obj())
    }
  }

  def distinctQuestions(course: String, term_year: String) = Action.async {
    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "question",
      "query" -> BSONDocument( "course" -> course, "year" -> year, "term" -> term)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) =>
        Logger.debug("Result: " + v.values)
        Ok(BSONArrayFormat.writes(v))
      case None =>
        Logger.debug("Result: no years found.")
        Ok(Json.obj())
    }
  }
}