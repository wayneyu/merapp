package controllers

import controllers.Application._
import models.{April, December, Term, Question}
import play.Routes
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Success, Failure}
import scala.collection.mutable.ListBuffer

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

  val collection = db[BSONCollection]("questions")

  def questions = Action.async {
    val coursesResult = distinctCourses()
    val yearsResult = distinctYears()

    val courseAndYearResult = for {
      cr <- coursesResult
      yr <- yearsResult
    } yield (cr, yr)

    coursesResult onComplete {
      case Success(res) => Logger.info("No. of courses found: " + res.length.toString())
      case Failure(exception) =>
    }
    yearsResult onComplete {
      case Success(res) => Logger.info("No. of years found: " + res.length.toString())
      case Failure(exception) =>
    }

    courseAndYearResult.map { case (courseList, yearList) =>
      Ok(views.html.question(Question.empty, false)(courseList, Nil, yearList, Nil, "", "", "", ""))
    }
  }

  def questionQuery(course: String, term_year: String, q:String): Future[List[BSONDocument]] = {
    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt

    // let's do our query
    val cursor: Cursor[BSONDocument] = collection.
    // find all people with name `name`
    find(BSONDocument("course" -> course, "term" -> term, "year" -> year, "question" -> q)).
    // perform the query and get a cursor of JsObject
    cursor[BSONDocument]

    // gather all the JsObjects in a list
    val question: Future[List[BSONDocument]] = cursor.collect[List]()
    question
  }

  def questionInJson(course: String, term_year: String, q: String) = Action.async {
    questionQuery(course, term_year, q).map( l => Ok(BSONArray.pretty(BSONArray(l))) )
  }

  def questionEdit(course: String, term_year: String, q: String) = question(course, term_year, q, true)

  def question(course: String, term_year: String, q: String, editable: Boolean = false) = Action.async {
    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt

    val question = questionQuery(course, term_year, q)
    val coursesResult = distinctCourses()
    val yearsResult = distinctYears()

    val res = for {
      cr <- coursesResult
      yr <- yearsResult
      q <- question
    } yield (cr, yr, q)

    res.map { case (courseList, yearList, question) =>
      {
        question match {
          case j :: js =>
            Logger.debug("No. of questions found: " + question.length.toString())
            val Q = j.as[Question]
            Ok(views.html.question(Q, editable)(courseList, Nil, yearList, Nil, course, year.toString, term, q))
          case Nil =>
            Ok(views.html.question(Question.empty, editable)(courseList, Nil, yearList, Nil, course, year.toString, term, q))
        }
      }
    }
  }

  def question(course: String, term: String, year: Int, q: String): Action[AnyContent] = question(course, term+"_"+year, q)

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

  def exam(course: String, term_year: String) = Action.async {
    Logger.info("Find exam  = " + course + " " + term_year)
    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt

    val cursor = collection.
      find(BSONDocument("course" -> course, "year" -> year, "term" -> term)).
      sort(BSONDocument("question" -> 1)).
      cursor[BSONDocument]

    val exams = cursor.collect[List]()

    exams.map{ list => Ok(BSONArray.pretty(BSONArray(list))) }
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
    Logger.info("distinctYears(course-> " + course + ", term-> " + term + ")")
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "year",
      "query" -> BSONDocument( "course" -> course, "term" -> term)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) =>
        Logger.info("Result: " + v.toString())
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
      case Some(list) => list.toList.sorted.map(_.toString())
      case None => Nil
    }
  }

  def distinctTermsList(course: String): Future[List[String]] = {
    // set up query
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "term", "query" -> BSONDocument( "course" -> course)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[List[String]]("values")
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
        Logger.info("Result: " + v.toString())
        Ok(BSONArrayFormat.writes(v))
      case None =>
        Logger.info("Result: no courses found.")
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

  def searchById(id: String) = Action.async {
    Logger.info("search question with id = " + id)
    val cursor = collection.
      // find with case-insensitive and use . to match any chars options
      find( BSONDocument("_id" -> BSONObjectID(id))).
      cursor[BSONDocument]

    val result: Future[List[BSONDocument]] = cursor.collect[List]()

    result.map( list => Ok(BSONArray.pretty(BSONArray(list))) )
  }

  def searchByKeywords(searchString: String) = Action.async {
    val res = searchByKeywordsQuery(searchString)
    res.map{ l =>
      Ok (views.html.search( l.map( e => e.as[Question].url)))
    }
  }

  def searchByKeywordsSubmit() = Action { request =>
    val form = request.body.asFormUrlEncoded
    form match {
      case Some(map) => {
        val searchString = map.getOrElse("searchString", Seq(""))(0)
        Redirect( routes.QuestionController.searchByKeywords(searchString) )
      }
      case None => Ok("")
    }
  }

  def searchByKeywordsQuery(searchString: String): Future[List[BSONDocument]] = {
    Logger.info("searching for " + searchString)

    val searchCommand = BSONDocument(
      "text" -> collection.name,
      "search" -> searchString
//      "filter" -> BSONDocument("year"->2013)
    )

    val result : Future[BSONDocument]= db.command(RawCommand(searchCommand))
    result.map {
      doc => doc.getAs[List[BSONDocument]]("results") match {
        case Some(list) => list.map( _.getAs[BSONDocument]("obj").get )
        case None => Nil
      }
    }

  }

  def bson2url(doc: BSONDocument) = Json.toJson(doc).as[Question].url

  def toJsArray(jsoList: List[JsValue]): JsArray = {
    jsoList.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
  }

  def findAndModify(course: String, term_year: String, q:String) = Action.async(parse.json) { request =>

    val ty = term_year.split("_")
    Logger.info("Editing " + course + "/" + ty(0) + "/" + ty(1) + "/" + q)

    val bson = BSONDocumentFormat.reads(request.body)

    bson match {

      case e: JsError => Future(BadRequest("Invalid JSON is passed."))

      case b: JsSuccess[BSONDocument] =>
      {
        val selector = BSONDocument(
          "course" -> course, "term" -> ty(0),
          "year" -> ty(1).toInt, "question" -> q)

        val modifier = BSONDocument(
          "$set" -> b.get)

        val command = FindAndModify(
          collection.name,
          selector,
          Update(modifier, true))

        val result = db.command(command)

        result.map( r =>
          r match {
            case Some(doc) =>
//              Ok(BSONDocument.pretty(doc))
              Redirect(routes.QuestionController.questionEdit(course, term_year, q))
            case None =>
              Ok("Question was not updated.")
          }
        )

      }
    }

//    val command = RawCommand(BSONDocument(
//      "distinct" -> "questions",
//      "key" -> "year", "query" -> BSONDocument( "course" -> course)))
//    val result = db.command(command) // result is Future[BSONDocument]
////    db.questions.findAndModify({ query: {_id: ObjectId("54559b01523146ee4ed13f2b")}, update:{$set:{year:2013}},new:true})

  }

  def examsForCourse(course: String) = Action {


    // val terms = distinctTermsList(course)  // HELP: Returns Future[List[String]] but List[String] is needed
    val terms = List("December")
    /* var exams = new ListBuffer[String]()
    for (term <- terms) {  // HELP: Scala does not like this either
      for ( year <- distinctYears(course, term) ) {
        exams += term + "_" + year.toString()
      }
    }*/
    val exams = List("December_2012", "December_2013")

    Ok(views.html.course(course, exams))
  }

  def questionsForExam(course: String, term_year: String) = Action {

    val questions = List("Question_01_(a)", "Question_01_(b)", "Question_02_(a)")
    Ok(views.html.exam(course, term_year, questions))
  }

  def browse() = Action.async{
    val allyears = distinctYears()
    val allcourses = distinctCourses()
    val exams = distinctExams()

    val res = for{
      c <- allcourses
      y <- allyears
      e <- exams
    } yield (c, y, e)

    res.map{
      ex => Ok(views.html.browse(ex._1, ex._2, ex._3))
    }

//    Logger.info("browsing questions")
//    val cursor: Cursor[JsObject] = collection.
//      // find with case-insensitive and use . to match any chars options
//      find( Json.obj("_id" -> BSONObjectID(id))).
//      cursor[JsObject]
//
//    val result: Future[List[JsObject]] = cursor.collect[List]()
//
//    result.map( arr => Ok(toJsArray(arr)) )

  }

  def distinctExams(): Future[List[(String, String, String)]] = {

    val command =
      BSONDocument(
        "aggregate" -> "questions", // we aggregate on collection `orders`
        "pipeline" -> BSONArray(
          BSONDocument(
            "$group" ->
              BSONDocument( "_id" -> BSONDocument("course"->"$course","year"->"$year","term"->"$term"))
          )
        )
      )

    val result = db.command(RawCommand(command))

    result.map( doc => doc.getAs[List[BSONDocument]]("result") )
      .map {
        case Some(list) =>
          list.map{
            d => d.getAs[BSONDocument]("_id").get
          }.map{
            d => (d.getAs[String]("course").get, d.getAs[Int]("year").get.toString, d.getAs[String]("term").get)
          }
        case None => Nil
      }

  }


//  def distinctExams() = Action.async {
//
//    val command =
//      BSONDocument(
//        "aggregate" -> "questions", // we aggregate on collection `orders`
//        "pipeline" -> BSONArray(
//          BSONDocument(
//            "$group" ->
//              BSONDocument( "_id" -> BSONDocument("course"->"$course","year"->"$year","term"->"$term"))
//          )
//        )
//      )
//
//    //    val command = Aggregate(collection.name, Seq(GroupMulti("course" -> "course","year" -> "year", "term" -> "term")(_)))
//
//    val result = db.command(RawCommand(command))
//
//
//    result.map( doc => Ok(BSONDocument.pretty(doc)))
//
//  }
}