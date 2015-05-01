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
		val coursesResult = distinctCourses()

		val courseAndYearResult = for {
			cr <- coursesResult
			tr <- distinctTerms(cr(0))
			yr <- distinctYears(cr(0), tr(0))
			qr <- distinctQuestions(cr(0), getTermYear(tr(0), yr(0)))
		} yield (cr, tr, yr, qr)

		courseAndYearResult.map { case (courseList, termList, yearList, questionList) =>
			Ok(views.html.question(Question.empty, false)(courseList, termList, yearList, questionList, "", "", "", "", None))
		}
	}

	private def questionResult(course: String, term_year: String, q: String, editable: Boolean = false)
	                          (implicit context: AppContext[AnyContent]): Future[Result] = {
		val (term: String, year: Int) = getTermAndYear(term_year)

		val question = MongoDAO.questionQuery(course, term_year, q)
		val termsResult = distinctTerms(course)
		val coursesResult = distinctCourses()
		val yearsResult = distinctYears(course, term)
		val questionsResult = distinctQuestions(course, term_year)
		val ratingResult = context.user match {
			case Some(u) => userRating(u, course, term_year, q)
			case None => Future(None)
		}

		val res = for {
			cr <- coursesResult
			tr <- termsResult
			yr <- yearsResult
			q <- question
			qr <- questionsResult
			r <- ratingResult
		} yield (cr, yr, q, qr, r)

		res.map { case (courseList, yearList, question, questionsList, rating) => {
			question match {
				case j :: js =>
					Logger.debug("No. of questions found: " + question.length.toString())
					val Q = j.as[Question]
					Ok(views.html.question(Q, editable)(courseList, Nil, yearList, questionsList, course, year.toString, term, q, rating))
				case Nil =>
					Ok(views.html.question(Question.empty, editable)(courseList, Nil, yearList, Nil, course, year.toString, term, q, None))
			}
		}
		}
	}

	def question(course: String, term_year: String, q: String) = UserAwaredAction.async { implicit context =>
		questionResult(course, term_year, q, false)
	}

	def questionEdit(course: String, term_year: String, q: String) = ContributorAction.async { implicit context =>
		questionResult(course, term_year, q, true)
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

	def getTermAndYear(term_year: String): (String, Int) = {
		val tysplit = term_year.split("_")
		val term = tysplit(0)
		val year = tysplit(1).toInt
		(term, year)
	}

	def getTermYear(term: String, year: String) = term + "_" + year

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
		MongoDAO.distinctQuestions(course, term_year).map {
			st => st.map(d => d.getAs[String]("question").get).sortWith(questionSort).toList
		}
	}

	def distinctQuestionsJSON(course: String, term_year: String) = Action.async {
		MongoDAO.distinctQuestions(course, term_year).map {
			st => Ok(BSONArrayFormat.writes(
				BSONArray(st.map(d => d.getAs[BSONString]("question").get).sortWith(questionSort)
				)))
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
/*

			Action.async {
			MongoDAO.getContributors().map {
				st => Ok(BSONArrayFormat.writes(
				BSONArray(st.map(d => d.getAs[BSONString]("contributors").get).sortWith(questionSort))
				))
			}
		}
*/

		/*
		MongoDAO.numberOfGoodQualitySolutions().map {
			st => st(BSONArrayFormat.writes(
			BSONArray(st.map(d => d.getAs[BSONString]("question").get))
			))
		}
		*/


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

		def prep(nprefix: Int, str: String, npostfix: Int) = prefix * nprefix + str + postfix * npostfix

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
		val topics = MongoDAO.topicsForCourse(course)

		topics.map {
			st => Ok(BSONArrayFormat.writes(BSONArray(
				st.map(d => d.getAs[BSONArray]("topics").getOrElse(BSONArray(Nil)))
			)))
		}
	}

	def examQuestions(course: String, term_year: String): Future[List[BSONDocument]] = {
		MongoDAO.examQuestions(course, term_year).map {
			_.toList.sortWith(questionSort)
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

	def updatedQualityFlag(course: String, term_year: String, q: String, newQualityFlag: String) = ContributorAction.async { implicit context =>
		MongoDAO.updateQualityFlag(course, term_year, q, newQualityFlag).map {
		o => Ok(BSONArrayFormat.writes(o.getOrElse(BSONArray())))
	}
	}
}