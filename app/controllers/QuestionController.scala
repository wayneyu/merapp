package controllers

import controllers.Application._
import models.{Question}
import play.Routes
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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

  val collection: JSONCollection = db.collection[JSONCollection]("questions")

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

  def questionQuery(course: String, term_year: String, q:String): Future[List[JsObject]] = {
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
    question
  }

  def questionInJson(course: String, term_year: String, q: String) = Action.async {
    questionQuery(course, term_year, q).map( l => Ok(toJsArray(l)) )
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

  def distinctTerms(course: String, year: Int) = Action.async {
    Logger.info("distinctTerms(course -> " + course + ", year-> " + year + ")")
    val command = RawCommand(BSONDocument("distinct" -> "questions", "key" -> "term",
      "query" -> BSONDocument( "course" -> course, "year" -> year)))
    val result = db.command(command) // result is Future[BSONDocument]

    result.map( doc => doc.getAs[BSONArray]("values")
    ).map {
      case Some(v) =>
        Logger.info("Result: " + v.toString())
        Ok(BSONArrayFormat.writes(v))
      case None =>
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
    val cursor: Cursor[JsObject] = collection.
      // find with case-insensitive and use . to match any chars options
      find( Json.obj("_id" -> BSONObjectID(id))).
      cursor[JsObject]

    val result: Future[List[JsObject]] = cursor.collect[List]()

    result.map( arr => Ok(toJsArray(arr)) )
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

//    val command = Aggregate(collection.name, Seq(
////      Match(BSONDocument("year"->2011))
//      Match(BSONDocument("text" -> collection.name , "search"->searchString))
////      Sort( Seq(Descending( "score:{$meta: \"textScore\"}}")) )
//    ))
//    val result = db.command(command)

//    val result = db.command(RawCommand(command.makeDocuments))//.map(_.toSeq)
//    result.map( seq => Logger.info("number of results: " + seq.size))
//    result.map( seq => Ok(views.html.search(seq.map( bsonDoc2Json ))) )
//    result.map( doc => Ok(BSONDocument.pretty(doc)) )

//    val cursor: Cursor[JsObject] = collection.
      // find all people with name `name`
//      find(Json.obj("text" -> collection.name, "search" -> searchString)).
//      sort(Json.obj("year" -> Json.obj("$gt" -> 2010) )).
//      sort(Json.obj("score" -> Json.obj("meta" -> "textScore") )).
      // perform the query and get a cursor of JsObject
//      cursor[JsObject]
//    val res: Future[List[JsObject]] = cursor.collect[List]()
//
//    result.map( d=>Ok(BSONDocument.pretty(d)) )

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
              Ok(BSONDocument.pretty(doc))
//              Redirect(routes.QuestionController.questionEdit(course, term_year, q))
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
    var exams = List("December_2012", "December_2013")

    Ok(views.html.course(course, exams))
  }

  def questionsForExam(course: String, term_year: String) = Action {

    val questions = List("Question_01_(a)", "Question_01_(b)", "Question_02_(a)")
    Ok(views.html.exam(course, term_year, questions))
  }
}