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

  def questions = Action.async { implicit request =>
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
    val (term: String, year: Int) = getTermAndYear(term_year)

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

  def getTermAndYear(term_year: String): (String, Int) = {
    val tysplit = term_year.split("_")
    val term = tysplit(0)
    val year = tysplit(1).toInt
    (term, year)
  }

  def questionInJson(course: String, term_year: String, q: String) = Action.async {
    questionQuery(course, term_year, q).map( l => Ok(BSONArray.pretty(BSONArray(l))) )
  }

  def questionEdit(course: String, term_year: String, q: String) = question(course, term_year, q, true)

  def question(course: String, term_year: String, q: String, editable: Boolean = false) = Action.async { implicit request =>
    val (term: String, year: Int) = getTermAndYear(term_year)

    val question = questionQuery(course, term_year, q)
    val coursesResult = distinctCourses()
    val yearsResult = distinctYears()
    val questionsResult = examQuestions(course, term_year)

    val res = for {
      cr <- coursesResult
      yr <- yearsResult
      q <- question
      qr <- questionsResult.map( l => l.map( _.as[Question].question ) )
    } yield (cr, yr, q, qr)

    res.map { case (courseList, yearList, question, questionsList) =>
      {
        question match {
          case j :: js =>
            Logger.info("No. of questions found: " + question.length.toString())
            val Q = j.as[Question]
            Ok(views.html.question(Q, editable)(courseList, Nil, yearList, questionsList, course, year.toString, term, q))
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
    val command = Aggregate(collection.name, Seq(
      GroupField("course")("course" -> First("course")),
      Sort(Seq(Ascending("course"))),
      Project("_id"->BSONInteger(0), "course" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => st.map( d => d.getAs[String]("course").get ).toList
    }
  }

  def distinctCourses(year: Int): Future[List[String]] = {

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("year" -> year)),
      GroupField("course")("course" -> First("course")),
      Sort(Seq(Descending("course"))),
      Project("_id"->BSONInteger(0), "course" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => st.map( d => d.getAs[String]("course").get ).toList
    }

  }

  def distinctCourses(year: Int, term: String) = Action.async {
    Logger.info("distinctCourses(year -> " + year + ", term-> " + term + ")")

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("year" -> year, "term" -> term )),
      GroupField("course")("course" -> First("course")),
      Sort(Seq(Descending("course"))),
      Project("_id"->BSONInteger(0), "course" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => Ok(BSONArrayFormat.writes(BSONArray(
        st.map(d => d.getAs[BSONString]("course").get)
      )))
    }
  }


  def distinctYears(): Future[List[String]] = {
    val command = Aggregate(collection.name, Seq(
      GroupField("year")("year" -> First("year")),
      Sort(Seq(Descending("year"))),
      Project("_id"->BSONInteger(0), "year" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => st.map(d => d.getAs[Int]("year").get.toString).toList
    }
  }

  def distinctYears(course: String): Future[List[String]] = {
    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course)),
      GroupField("year")("year" -> First("year")),
      Sort(Seq(Descending("year"))),
      Project("_id"->BSONInteger(0), "year" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => st.map(d => d.getAs[Int]("year").get.toString()).toList
    }

  }

  def distinctYears(course: String, term: String) = Action.async {
    Logger.info("distinctYears(course-> " + course + ", term-> " + term + ")")

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course, "term" -> term )),
      GroupField("year")("year" -> First("year")),
      Sort(Seq(Descending("year"))),
      Project("_id"->BSONInteger(0), "year" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
         st => Ok(BSONArrayFormat.writes(BSONArray(
           st.map(d => d.getAs[BSONInteger]("year").get)
         )))
      }

  }

  def distinctTerms(): Future[List[String]] = {
    val command = Aggregate(collection.name, Seq(
      GroupField("term")("term" -> First("term")),
      Sort(Seq(Ascending("term"))),
      Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => st.map(d => d.getAs[String]("term").get).toList
    }
  }

  def distinctTermsList(course: String): Future[List[String]] = {
    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course)),
      GroupField("term")("term" -> First("term")),
      Sort(Seq(Ascending("term"))),
      Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => st.map(d => d.getAs[String]("term").get).toList
    }

  }

  def distinctTerms(course: String) = Action.async {

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course)),
      GroupField("term")("term" -> First("term")),
      Sort(Seq(Ascending("term"))),
      Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => Ok(BSONArrayFormat.writes(BSONArray(
        st.map(d => d.getAs[BSONString]("term").get)
      )))
    }

  }

  def distinctTerms(course: String, year: String) = Action.async {
    Logger.info("distinctYears(course-> " + course + ", year-> " + year + ")")
    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course, "year" -> year)),
      GroupField("term")("term" -> First("term")),
      Sort(Seq(Ascending("term"))),
      Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{
      st => Ok(BSONArrayFormat.writes(BSONArray(
        st.map(d => d.getAs[BSONString]("term").get)
      )))
    }
  }

  def distinctQuestions(course: String, term_year: String) = Action.async {
    Logger.info("Find exam  = " + course + " " + term_year)
    val (term: String, year: Int) = getTermAndYear(term_year)

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course, "year" -> year, "term" -> term)),
      GroupField("question")("question"->First("question")),
      Sort(Seq(Ascending("question"))),
      Project("_id" -> BSONInteger(0), "question"->BSONInteger(1))
    ))

    val questions = db.command(command)

    questions.map{
      st => Ok(BSONArrayFormat.writes(
        BSONArray(st.map(d => d.getAs[BSONString]("question").get).sortWith(questionSort)
      )))
    }

  }

  def questionSort(tis: BSONDocument, tat: BSONDocument): Boolean = {
    questionSort(tis.getAs[String]("question").get, tat.getAs[String]("question").get)
  }

  def questionSort(tis: BSONString, tat: BSONString): Boolean = {
    questionSort(tis.value, tat.value)
  }

  def questionSort(tis: String, tat: String): Boolean = {
    val pattern = ".*\\([a-zA-Z]+\\).*"
    val str1 = tis
    val str2 = tat
    def diff(s1: String, s2: String) = Math.abs(s1.size - s2.size)
    val postfix = " (a)"
    val prefix = "0"

    def prep(nprefix: Int, str: String,  npostfix: Int) = prefix*nprefix + str + postfix*npostfix

    def append(str1: String, str2: String): (String, String) = {
      val m1 = str1.matches(pattern)
      val m2 = str2.matches(pattern)
      if (!(m1 ^ m2)) (str1, str2)
      else if (m2) (prep(0, str1, 1), str2)
      else (str1, prep(0, str2, 1))
    }

    def prepend(str1: String, str2: String): (String, String) = {
      val n1 = str1.size
      val n2 = str2.size
      if (n1 < n2) (prep(diff(str1, str2), str1, 0), str2)
      else if (n1 > n2) (str1, prep(diff(str1, str2), str2, 0))
      else (str1, str2)
    }

    val (s1, s2) = append(str1, str2)
    val (ss1, ss2) = prepend(s1, s2)

    ss1 < ss2

  }

  def allTopicsCourse(course: String) = Action.async {
    val topics = topicsForCourse(course)

    topics.map {
      st => Ok(BSONArrayFormat.writes(BSONArray(
        st.map(d => d.getAs[BSONArray]("topics").getOrElse(BSONArray(Nil)))
      )))
    }
  }

  def examQuestions(course: String, term_year: String): Future[List[BSONDocument]] = {
    Logger.info("Find exam  = " + course + " " + term_year)
    val (term: String, year: Int) = getTermAndYear(term_year)

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course, "year" -> year, "term" -> term)),
      Sort(Seq(Ascending("question")))
    ))

    val questions = db.command(command)

    questions.map{ _.toList.sortWith(questionSort) }
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

    val (term, year) = getTermAndYear(term_year)
    Logger.info("Editing " + course + "/" + term + "/" + year + "/" + q)

    val bson = BSONDocumentFormat.reads(request.body)

    bson match {

      case e: JsError => Future(BadRequest("Invalid JSON passed."))

      case b: JsSuccess[BSONDocument] =>
      {
        val selector = BSONDocument(
          "course" -> course, "term" -> term,
          "year" -> year.toInt, "question" -> q)

        val modifier = BSONDocument(
          "$set" -> b.get)

        val command = FindAndModify(
          collection.name,
          selector,
          Update(modifier, true))

        val result = db.command(command)

        result.map{
            case Some(doc) =>
//              Ok(BSONDocument.pretty(doc))
              Redirect(routes.QuestionController.questionEdit(course, term_year, q))
            case None =>
              Ok("Question was not updated.")
          }

      }
    }

  }

  def examsForCourse(course: String): Future[List[BSONDocument]] = {

    Logger.info("Find exams for " + course)

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course)),
      GroupMulti("year" -> "year", "term" -> "term")("year" -> First("year"), "term" -> First("term")),
      Sort(Seq(Descending("year"), Ascending("term"))),
      Project("_id"->BSONInteger(0), "year" -> BSONInteger(1), "term" -> BSONInteger(1))
    ))

    val res = db.command(command)

    res.map{ _.toList }

  }

  def topicsForCourse(course: String): Future[List[BSONDocument]] = {
    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("course" -> course)),
      Project("_id"->BSONInteger(0), "topics" -> BSONInteger(1), "term" -> BSONInteger(1))
    ))

    val topics = db.command(command)

    topics.map{ _.toList }
  }

  def questionsForTopic(topic: String): Future[List[BSONDocument]] = {

    val command = Aggregate(collection.name, Seq(
        Match(BSONDocument("topics" -> BSONDocument("$in" -> BSONArray(topic)))),
        Sort(Seq(Ascending("course"), Ascending("year"), Ascending("term"), Ascending("question")))
      )
    )

    val questions = db.command(command)

    questions.map{ _.toList }
  }

  def course(course: String) = Action.async {
    val exams = examsForCourse(course)
    val topics = topicsForCourse(course)
    val num_questions = topics.map{ ts => ts.size }

    val list_of_topics = topics.map{
      l => l.flatMap(
        doc => doc.getAs[List[String]]("topics").getOrElse(List("no_topic_specified"))
      ).toList.sorted
    }

    val res = for {
      e <- exams
      t <- list_of_topics
      num_questions <- num_questions
    } yield (e, t, num_questions)

    res.map{ case(exams, topics, num_questions) =>
      Logger.info(topics mkString "," )
      Ok(views.html.course(course,
        exams.map{
          d => d.getAs[String]("term").get + "_" + d.getAs[Int]("year").get.toString
        }, topics, num_questions
      ))
    }
  }


  def exam(course: String, year: String, term: String): Action[AnyContent] = exam(course, term + "_" + year)

  def exam(course: String, term_year: String) = Action.async {
    val questions = examQuestions(course, term_year)

    questions.map{
        l => Ok(views.html.exam(course, term_year, l.map( d => d.as[Question] )) )
      }
  }

  def exams() = Action.async{
    val allyears = distinctYears().map(_.sorted)
    val allcourses = distinctCourses()
    val exams = distinctExams()

    val res = for{
      c <- allcourses
      y <- allyears
      e <- exams
    } yield (c, y, e)

    res.map{
      ex => Ok(views.html.exams(ex._1, ex._2, ex._3))
    }
  }

  def distinctExams(): Future[List[(String, String, String)]] = {

    val command = Aggregate(collection.name, Seq(
      GroupMulti("course" -> "course", "year" -> "year", "term" -> "term")
        ("course" -> First("course"), "year" -> First("year"), "term" -> First("term")),
      Project("_id"->BSONInteger(0), "course" -> BSONInteger(1), "year" -> BSONInteger(1), "term" -> BSONInteger(1))
    ))

    val exams = db.command(command)

    exams.map{
      st => st.map(
        d => (d.getAs[String]("course").get, d.getAs[Int]("year").get.toString, d.getAs[String]("term").get)
      ).toList
    }
  }

  def upload(path: String) = Action(parse.multipartFormData) { request =>
    request.body.file("file").map { file =>
      import java.io.File
      val FILE_FOLDER = "public/raw_database/json_data/"
      val filename = file.filename
      val pattern = "questions\\/(.*?\\/.*?)\\/".r
      val subfolder = pattern.findFirstMatchIn(path).map(m => m.group(1)).getOrElse("")
      val to = new File(FILE_FOLDER + subfolder, filename)
      Logger.info("URL: " + path + " Uploading " + filename + " " + file.contentType + " Moving image to " + to.getCanonicalPath)
      file.ref.moveTo(to, true)
      Ok("File uploaded to " + to.getPath)
    }.getOrElse {
      BadRequest("Image missing")
    }
  }
}