package controllers

import java.util.{Calendar, Date}

import models._
import play.api._
import play.api.libs.Files.TemporaryFile
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import securesocial.core.RuntimeEnvironment
import service._

import scala.collection.immutable.Stream.Empty
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

import reactivemongo.api._
import reactivemongo.core.commands._

import play.modules.reactivemongo.MongoController

/**
 * Created by wayneyu on 11/1/14.
 */
object QuestionController extends ServiceComponent with MongoController {

	def questions = UserAwaredAction.async { implicit context =>
		val courseAndYearResult = for {
			cr <- distinctCourses()
			tr <- distinctTerms(cr.head)
			yr <- distinctYears(cr.head, tr.head)
			nr <- distinctQuestions(cr.head, tr.head, yr.head)
		} yield (cr, tr, yr, nr)

		courseAndYearResult.map { case (courseList, termList, yearList, numberList) =>
			Logger.debug(courseList toString)
			Logger.debug(termList toString)
			Logger.debug(yearList toString)
			Logger.debug(numberList toString)
			Ok(views.html.question(Question.empty, editable = false)(courseList, termList, yearList, numberList, "", "", "", "", None))
		}
	}

	private def questionResult(course: String, term_year: String, number: String, editable: Boolean = false)
	                          (implicit context: AppContext[AnyContent]): Future[Result] = {
		val (term: String, year: Int) = assets.term_and_year_from_term_year(term_year)

		val ratingResult = context.user match {
			case Some(u) => userRating(u, course, term_year, number)
			case None => Future(None)
		}

		val res = for {
			cr <- distinctCourses()
			tr <- distinctTerms(course)
			yr <- distinctYears(course, term)
			q <- MongoDAO.questionQuery(course, term_year, number)
			nr <- distinctQuestions(course, term_year)
			r <- ratingResult
		} yield (cr, yr, q, nr, r)

		res.map { case (courseList, yearList, question, questionsList, rating) =>
			question match {
				case j :: js =>
					Logger.debug("No. of questions found: " + question.length.toString())
					val Q = j.as[Question]
					Logger.debug(Q.url)
					Ok(views.html.question(Q, editable)(courseList, Nil, yearList, questionsList, course, year.toString, term, number, rating))
				case Nil =>
					Ok(views.html.question(Question.empty, editable)(courseList, Nil, yearList, Nil, course, year.toString, term, number, None))
			}
		}
	}


	def question(course: String, term_year: String, q: String) = UserAwaredAction.async { implicit context =>
		questionResult(course, term_year, q, editable = false)
	}

	def questionEdit(course: String, term_year: String, q: String) = ContributorAction.async { implicit context =>
		questionResult(course, term_year, q, editable = true)
	}

	def questionFindAndModify(course: String, term_year: String, q: String) = ContributorAction.async(parse.json) { implicit context: AppContext[JsValue] =>
		Logger.debug("Editing " + course + "/" + term_year + "/" + q)

		val bson = BSONDocumentFormat.reads(context.request.body)

		bson match {
			case e: JsError => Future(BadRequest("Invalid JSON passed."))
			case b: JsSuccess[BSONDocument] => {
				MongoDAO.questionFindAndModify(course, term_year, q, b.get).map {
					case Some(doc) =>
						Redirect(routes.QuestionController.questionEdit(course, term_year, q))
					case None =>
						BadRequest("Question was not updated.")
				}
			}
		}
	}

	def upload(path: String) = ContributorAction.async(parse.multipartFormData) { implicit context: AppContext[MultipartFormData[TemporaryFile]] =>
		context.request.body.file("file").map { file =>
			import java.io.File
			val FILE_FOLDER = "public/raw_database/json_data/"
			val filename = file.filename
			val pattern = "questions\\/(.*?\\/.*?)\\/".r
			val subfolder = pattern.findFirstMatchIn(path).map(m => m.group(1)).getOrElse("")
			val to = new File(FILE_FOLDER + subfolder, filename)
			Logger.debug("URL: " + path + " Uploading " + filename + " " + file.contentType + " Moving image to " + to.getCanonicalPath)
			file.ref.moveTo(to, true)
			Future(Ok("File uploaded to " + to.getPath))
		}.getOrElse {
			Future(BadRequest("Image missing"))
		}
	}

	def course(course: String) = UserAwaredAction.async { implicit context =>
		val exams = MongoDAO.examsForCourse(course)
		val topics = MongoDAO.topicsForCourse(course)
		val num_questions = topics.map {
			_.size
		}

		val list_of_topics = topics.map {
			l => l.flatMap(
				doc => doc.getAs[List[String]]("topics").getOrElse(List("no_topic_specified"))
			).toList.sorted
		}

		val res = for {
			e <- exams
			t <- list_of_topics
			num_questions <- num_questions
		} yield (e, t, num_questions)

		res.map { case (exams, topics, num_questions) =>
			Logger.debug(topics mkString ",")
			Ok(views.html.course(course,
				exams.map {
					d => d.getAs[String]("term").get + "_" + d.getAs[Int]("year").get.toString
				}.toList, topics, num_questions
			))
		}
	}

	def searchByKeywords(searchString: String) = UserAwaredAction.async { implicit context =>
		val res = searchByKeywordsQuery(searchString)
		res.map { l =>
			Ok(views.html.search(l.map(e => e.as[SearchResult])))
		}
	}

	def exam(course: String, term_year: String) = UserAwaredAction.async { implicit context =>
		val questions = examQuestions(course, term_year)

		questions.map {
			l => Ok(views.html.exam(course, term_year, l.map(d => d.as[Question])))
		}
	}

	def exams() = UserAwaredAction.async { implicit context =>
		val allyears = distinctYears().map(_.sorted)
		val allcourses = distinctCourses()
		val exams = distinctExams()

		val res = for {
			c <- allcourses
			y <- allyears
			e <- exams
		} yield (c, y, e)

		res.map {
			ex => Ok(views.html.exams(ex._1, ex._2, ex._3))
		}
	}

	def questionInJson(course: String, term_year: String, q: String) = Action.async {
		MongoDAO.questionQuery(course, term_year, q).map(l => Ok(BSONArray.pretty(BSONArray(l))))
	}

	def question(course: String, term: String, year: Int, q: String): Action[AnyContent] = question(course, term + "_" + year, q)

	def questionSubmit() = Action { request =>
		val form = request.body.asFormUrlEncoded
		form match {
			case Some(map) => {
				Redirect(controllers.routes.QuestionController.question(
					map.getOrElse("course", Seq(""))(0),
					map.getOrElse("term", Seq(""))(0) + "_" + map.getOrElse("year", Seq(""))(0),
					map.getOrElse("question", Seq(""))(0)))
			}
			case None => Redirect(controllers.routes.QuestionController.question("", "", ""))
		}
	}

	def distinctCourses(): Future[List[String]] = {
		MongoDAO.distinctCourses().map {
			st => st.map(d => d.getAs[String]("course").get).toList
		}
	}

	def distinctCourses(year: Int): Future[List[String]] = {
		MongoDAO.distinctCourses(year).map {
			st => st.map(d => d.getAs[String]("course").get).toList
		}
	}

	def distinctCoursesJSON(year: Int, term: String) = Action.async {
		MongoDAO.distinctCourses(year, term).map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(d => d.getAs[BSONString]("course").get)
			)))
		}
	}

	def distinctYears(): Future[List[String]] = {
		MongoDAO.distinctYears().map {
			st => st.map(d => d.getAs[Int]("year").get.toString).toList
		}
	}

	def distinctYears(course: String): Future[List[String]] = {
		MongoDAO.distinctYears(course).map {
			st => st.map(d => d.getAs[Int]("year").get.toString()).toList
		}
	}

	def distinctYears(course: String, term: String): Future[List[String]] = {
		MongoDAO.distinctYears(course, term).map {
			st => st.map(d => d.getAs[Int]("year").get.toString()).toList
		}
	}

	def distinctYearsJSON(course: String, term: String) = Action.async {
		MongoDAO.distinctYears(course, term).map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(d => d.getAs[BSONInteger]("year").get)
			)))
		}
	}

	def distinctTerms(): Future[List[String]] = {
		MongoDAO.distinctTerms().map {
			st => st.map(d => d.getAs[String]("term").get).toList
		}
	}

	def distinctTerms(course: String): Future[List[String]] = {
		MongoDAO.distinctTerms(course).map {
			st => st.map(d => d.getAs[String]("term").get).toList
		}
	}

	def distinctTermsJSON(course: String) = Action.async {
		MongoDAO.distinctTerms(course).map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(d => d.getAs[BSONString]("term").get)
			)))
		}
	}

	def distinctTermsJSON(course: String, year: String) = Action.async {
		MongoDAO.distinctTerms(course, year).map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(d => d.getAs[BSONString]("term").get)
			)))
		}
	}

	def distinctQuestions(course: String, term_year: String): Future[List[String]] = {
		MongoDAO.distinct_question_numbers_for_course_and_term_year(course, term_year).map {
			st => st.map(d => d.getAs[String]("number").get).toList
		}
	}

	def distinctQuestions(course: String, term: String, year: String): Future[List[String]] = {
		distinctQuestions(course, assets.term_year_from_term_and_year(term, year))
	}

  def distinctQuestions(course: String, term: String, year: Int): Future[List[String]] = {
    distinctQuestions(course, assets.term_year_from_term_and_year(term, year))
  }

  def distinctQuestionsJSON(course: String, term_year: String) = Action.async {
		MongoDAO.distinct_question_numbers_for_course_and_term_year(course, term_year).map {
			st =>
//				Logger.debug(st.map(_.getAs[BSONString]("number").get).toString)
				Ok(BSONArrayFormat.writes(
				BSONArray(st.map( a => {
					BSONDocument(
						"number_human" -> a.getAs[BSONString]("number_human"),
						"number" -> a.getAs[BSONString]("number"))
					}))
				))
		}
	}


	def numberOfGoodQualitySolutions() = Action.async {
		MongoDAO.numberOfGoodQualitySolutions().map {
			st => Ok(BSONArrayFormat.writes(BSONArray(st)))
		}
	}


	def distinctContributors() = UserAwaredAction.async {
		implicit context => MongoDAO.
			distinctContributors().map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(
					d => d.getAs[BSONString]("contributors").getOrElse(BSONArray(Nil))
				)
			)
			))
		}
	}

	def allTopicsCourse(course: String) = Action.async {
		val topics = MongoDAO.topicsForCourse(course)

		topics.map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(d => d.getAs[BSONArray]("topics").getOrElse(BSONArray(Nil)))
			)))
		}
	}

	def examQuestions(course: String, term_year: String): Future[List[BSONDocument]] = {
		MongoDAO.list_question_BSONDocument_for_course_and_term_year(course, term_year).map {
			_.toList
		}
	}

	def searchById(id: String) = Action.async {
		MongoDAO.searchById(id).map(list => Ok(BSONArray.pretty(BSONArray(list))))
	}

	def searchByKeywordsSubmit() = Action { request =>
		val form = request.body.asFormUrlEncoded
		form match {
			case Some(map) => {
				val searchString = map.getOrElse("searchString", Seq(""))(0)
				Redirect(routes.QuestionController.searchByKeywords(searchString))
			}
			case None => Ok("")
		}
	}

	def searchByKeywordsQuery(searchString: String): Future[List[BSONDocument]] = {
		MongoDAO.searchByKeywordsQuery(searchString).map {
			_.toList
		}
	}

	def bson2url(doc: BSONDocument) = Json.toJson(doc).as[Question].url

	def toJsArray(jsoList: List[JsValue]): JsArray = {
		jsoList.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
	}

	def examsForCourse(course: String): Future[List[BSONDocument]] = {
		MongoDAO.examsForCourse(course).map {
			_.toList
		}
	}

	def exam(course: String, year: String, term: String): Action[AnyContent] = exam(course, term + "_" + year)

	def distinctExams(): Future[List[(String, String, String)]] = {
		MongoDAO.distinctExams().map {
			st => st.map(
				d => (d.getAs[String]("course").get, d.getAs[Int]("year").get.toString, d.getAs[String]("term").get)
			).toList
		}
	}

	def addTopic(course: String, term_year: String, q: String, topic: String) = ContributorAction.async { implicit context =>
		MongoDAO.addTopic(course, term_year, q, topic).map {
			o => Ok(BSONArrayFormat.writes(o.getOrElse(BSONArray())))
		}
	}

	def removeTopic(course: String, term_year: String, q: String, topic: String) = ContributorAction.async { implicit context =>
		MongoDAO.removeTopic(course, term_year, q, topic).map {
			o => Ok(BSONArrayFormat.writes(o.getOrElse(BSONArray())))
		}
	}

	def vote(course: String, term_year: String, question: String, rating: Int) = VisitorAction.async { implicit context =>
		val timestamp = Calendar.getInstance().getTimeInMillis

		val vote = Vote(context.user.get.userkey.key, Question.qid(course, term_year, question), timestamp, rating)
		val res = MongoDAO.insertVote(vote, course, term_year, question)

		res.map {
			case Some(doc) => Ok(BSONDocumentFormat.writes(doc))
			case None => BadRequest("Error adding vote")
		}
	}

	def userRating(user: User, course: String, term_year: String, question: String): Future[Option[Int]] = {
		val qid = Question.qid(course, term_year, question)
		val res = MongoDAO.getRating(user, qid)
		res.map {
			st => st.headOption.flatMap[Int] {
				_.getAs[Int]("rating")
			}
		}
	}

	def flags_per_exam() = Action.async { implicit context =>
//		Prepares json for donut chart for exams in progress on the dashboard
		val flags_per_exam = MongoDAO.flags_per_exam()

		val exam_flag_to_count = flags_per_exam.map { st =>
			for {
				s <- st.toSeq
				x <- s.getAs[BSONDocument]("_id")
				course <- x.getAs[String]("course")
				term <- x.getAs[String]("term")
				year <- x.getAs[Int]("year")
				course_term_year = course + "_" + term.replace("December", "Dec").replace("April", "Apr") + "_" + year.toString
				flag <- x.getAs[String]("flag")
				count <- s.getAs[Int]("num_questions")
			} yield (course_term_year, flag) -> count
		} map (sequence => sequence.toMap)

		val all_exams = flags_per_exam.map { st =>
			for {
				s <- st.toSeq
				x <- s.getAs[BSONDocument]("_id")
				course <- x.getAs[String]("course")
				term <- x.getAs[String]("term")
				year <- x.getAs[Int]("year")
				course_term_year = course + "_" + term.replace("December", "Dec").replace("April", "Apr") + "_" + year.toString
				url = "/exams/" + course + "/" + term + "_" + year.toString
			} yield (course_term_year, url)
		}

		var res: BSONArray = BSONArray.empty

		exam_flag_to_count.flatMap { get_count =>
			all_exams map { exams =>
				exams.distinct.sortWith(_._1 < _._1) foreach {
					case (exam, url) =>
						val CQ = get_count.getOrElse((exam, "CQ"), 0)
						val RQ = get_count.getOrElse((exam, "RQ"), 0)
						val QBQ = get_count.getOrElse((exam, "QBQ"), 0)
						val QGQ = get_count.getOrElse((exam, "QGQ"), 0)
						val CH = get_count.getOrElse((exam, "CH"), 0)
						val RH = get_count.getOrElse((exam, "RH"), 0)
						val QBH = get_count.getOrElse((exam, "QBH"), 0)
						val QGH = get_count.getOrElse((exam, "QGH"), 0)
						val CS = get_count.getOrElse((exam, "CS"), 0)
						val RS = get_count.getOrElse((exam, "RS"), 0)
						val QBS = get_count.getOrElse((exam, "QBS"), 0)
						val QGS = get_count.getOrElse((exam, "QGS"), 0)

						if (CQ + RQ + QBQ + CH + RH + QBH + CS + RS + QBS > 0)
							res = res add BSONDocument(
								"exam" -> exam.replace("_", " "),
								"url" -> url,
								"progress" -> BSONInteger(100*(2*QGQ + 5*QGH + 13*QGS + QBQ + 4*QBH + 12*QBS + RQ + 3*RH + 9*RS)/20/(CQ+RQ+QBQ+QGQ)),
								"Statements" -> List(QGQ, QBQ, RQ, CQ),
								"Hints" -> List(QGH, QBH, RH, CH),
								"Solutions" -> List(QGS, QBS, RS, CS)
							)
				}
				Ok(BSONArrayFormat.writes(res))
			}
		}
	}

	def dashboard_flag(flag: String) = UserAwaredAction.async { implicit context =>
		val questions = MongoDAO.questionsWithFlag(flag).map {
			_.toList
		}

		questions.map { st =>
			Ok(views.html.dashboard_flag(flag, st.map(q => q.as[Question])))
		}
	}


	def flags_to_human(flag: String): String = {
		// translates cryptic flag abbreviations to english
		flag match {
			case "CQ" => "Content for question statement"
			case "RQ" => "Review question statement"
			case "QBQ" => "Improve question statement"
			case "QGQ" => "Good quality question statement"
			case "CH" => "Content for hint"
			case "RH" => "Review hint"
			case "QBH" => "Improve hint"
			case "QGH" => "Good quality hint"
			case "CS" => "Content for solution"
			case "RS" => "Review solution"
			case "QBS" => "Improve solution"
			case "QGS" => "Good quality solution"
		}
	}


	def dashboard() = UserAwaredAction.async { implicit context =>
		val qualityFlags = List("R", "QB")
		val contentTypes = List("Q", "H", "S")

		val flags = for {
			q <- qualityFlags
			c <- contentTypes
		} yield q + c

		val res = Future.traverse(flags)(f => Future(flags_to_human(f)) zip MongoDAO.questionsWithFlagCount(f))

		res map { st =>
			Ok(views.html.dashboard(st))
		}
	}

	def updatedQualityFlag(course: String, term_year: String, q: String, newQualityFlag: String) = ContributorAction.async { implicit context =>
		MongoDAO.updateQualityFlag(course, term_year, q, newQualityFlag).map {
			o => Ok(BSONArrayFormat.writes(o.getOrElse(BSONArray())))
		}
	}
}