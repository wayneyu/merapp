package service

import models._
import play.api._
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import securesocial.core.BasicProfile
import service.User.UserWriter
import service.User.UserWriter
import views.html.course

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import reactivemongo.api._
import reactivemongo.core.commands._

import play.modules.reactivemongo.MongoController

/**
 * Created by wayneyu on 01/27/15.
 */
object MongoDAO extends Controller with MongoController {

	val questionCollection = db[BSONCollection]("questions")
	val topicsCollection = db[BSONCollection]("topics")
	val usersCollection = db[BSONCollection]("users")
	val profilesCollection = db[BSONCollection]("profiles")

	def questionQuery(course: String, term_year: String, q:String): Future[List[BSONDocument]] = {
		val (term: String, year: Int) = getTermAndYear(term_year)

		// let's do our query
		val cursor: Cursor[BSONDocument] = questionCollection.
			// find all people with name `name`
			find(BSONDocument("course" -> course, "term" -> term, "year" -> year, "question" -> q)).
			// perform the query and get a cursor of JsObject
			cursor[BSONDocument]

		// gather all the JsObjects in a list
		cursor.collect[List]()
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

	def distinctCourses(): Future[Stream[BSONDocument]] = {
		// set up query
		val command = Aggregate(questionCollection.name, Seq(
			GroupField("course")("course" -> First("course")),
			Sort(Seq(Ascending("course"))),
			Project("_id"->BSONInteger(0), "course" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctCourses(year: Int): Future[Stream[BSONDocument]] = {

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("year" -> year)),
			GroupField("course")("course" -> First("course")),
			Sort(Seq(Descending("course"))),
			Project("_id"->BSONInteger(0), "course" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctCourses(year: Int, term: String): Future[Stream[BSONDocument]] = {
		Logger.debug("distinctCourses(year -> " + year + ", term-> " + term + ")")

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("year" -> year, "term" -> term )),
			GroupField("course")("course" -> First("course")),
			Sort(Seq(Descending("course"))),
			Project("_id"->BSONInteger(0), "course" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctYears(): Future[Stream[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			GroupField("year")("year" -> First("year")),
			Sort(Seq(Descending("year"))),
			Project("_id"->BSONInteger(0), "year" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctYears(course: String): Future[Stream[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course)),
			GroupField("year")("year" -> First("year")),
			Sort(Seq(Descending("year"))),
			Project("_id"->BSONInteger(0), "year" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctYears(course: String, term: String): Future[Stream[BSONDocument]] = {
		Logger.debug("distinctYears(course-> " + course + ", term-> " + term + ")")

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course, "term" -> term )),
			GroupField("year")("year" -> First("year")),
			Sort(Seq(Descending("year"))),
			Project("_id"->BSONInteger(0), "year" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctTerms(): Future[Stream[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			GroupField("term")("term" -> First("term")),
			Sort(Seq(Ascending("term"))),
			Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctTermsList(course: String): Future[Stream[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course)),
			GroupField("term")("term" -> First("term")),
			Sort(Seq(Ascending("term"))),
			Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctTerms(course: String): Future[Stream[BSONDocument]] = {

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course)),
			GroupField("term")("term" -> First("term")),
			Sort(Seq(Ascending("term"))),
			Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctTerms(course: String, year: String): Future[Stream[BSONDocument]] = {
		Logger.debug("distinctTerms(course-> " + course + ", year-> " + year + ")")
		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course, "year" -> year)),
			GroupField("term")("term" -> First("term")),
			Sort(Seq(Ascending("term"))),
			Project("_id"->BSONInteger(0), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def distinctQuestions(course: String, term_year: String): Future[Stream[BSONDocument]] = {
		Logger.debug("Distinct questions  = " + course + " " + term_year)
		val (term: String, year: Int) = getTermAndYear(term_year)

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course, "year" -> year, "term" -> term)),
			GroupField("question")("question"->First("question")),
			Sort(Seq(Ascending("question"))),
			Project("_id" -> BSONInteger(0), "question"->BSONInteger(1))
		))

		db.command(command)
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

	def examQuestions(course: String, term_year: String): Future[List[BSONDocument]] = {
		Logger.debug("Exam questions for " + course + " " + term_year)
		val (term: String, year: Int) = getTermAndYear(term_year)

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course, "year" -> year, "term" -> term)),
			Sort(Seq(Ascending("question")))
		))

		val questions = db.command(command)

		questions.map{ _.toList.sortWith(questionSort) }
	}

	def searchById(id: String): Future[List[BSONDocument]] = {
		Logger.debug("search question with id = " + id)
		val cursor = questionCollection.
			// find with case-insensitive and use . to match any chars options
			find( BSONDocument("_id" -> BSONObjectID(id))).
			cursor[BSONDocument]

		cursor.collect[List]()
	}

	def searchByKeywordsQuery(searchString: String): Future[List[BSONDocument]] = {
		// Search for keywords in statement, solutions, hints, answers and topics field of Questions collection
		// Results are returned as an array of BSONDocument of {course: , questions:, statement_html: , term: , year: score:}
		Logger.debug("searching for " + searchString)

		val searchCommand = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("$text" -> BSONDocument("$search" -> searchString))),
			Project("_id"->BSONInteger(0), "textScore" -> BSONDocument("$meta" -> "textScore"), "course" -> BSONInteger(1),
				"year" -> BSONInteger(1), "term" -> BSONInteger(1), "question" -> BSONInteger(1), "statement_html" -> BSONInteger(1)),
			Sort(Seq(Descending("textScore")))
		))

		val result = db.command(searchCommand)

		result.map { _.toList }
	}

	def bson2url(doc: BSONDocument) = Json.toJson(doc).as[Question].url

	def toJsArray(jsoList: List[JsValue]): JsArray = {
		jsoList.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
	}

	def examsForCourse(course: String): Future[Stream[BSONDocument]] = {

		Logger.debug("Find exams for " + course)

		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course)),
			GroupMulti("year" -> "year", "term" -> "term")("year" -> First("year"), "term" -> First("term")),
			Sort(Seq(Descending("year"), Ascending("term"))),
			Project("_id"->BSONInteger(0), "year" -> BSONInteger(1), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def topicsForCourse(course: String): Future[Stream[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course)),
			Project("_id"->BSONInteger(0), "topics" -> BSONInteger(1), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def questionsForTopic(topic: String): Future[Stream[BSONDocument]] = {

		val command = Aggregate(questionCollection.name, Seq(
				Match(BSONDocument("topics" -> BSONDocument("$in" -> BSONArray(topic)))),
				Sort(Seq(Ascending("course"), Ascending("year"), Ascending("term"), Ascending("question")))
			))

		db.command(command)
	}

	def distinctExams(): Future[Stream[BSONDocument]] = {

		val command = Aggregate(questionCollection.name, Seq(
			GroupMulti("course" -> "course", "year" -> "year", "term" -> "term")
				("course" -> First("course"), "year" -> First("year"), "term" -> First("term")),
			Project("_id"->BSONInteger(0), "course" -> BSONInteger(1), "year" -> BSONInteger(1), "term" -> BSONInteger(1))
		))

		db.command(command)
	}

	def questionFindAndModify(course: String, term_year: String, q:String, bson: BSONDocument): Future[Option[BSONDocument]] = {
		val (term, year) = getTermAndYear(term_year)

		val selector = BSONDocument(
			"course" -> course, "term" -> term,
			"year" -> year.toInt, "question" -> q)

		val modifier = BSONDocument(
			"$set" -> bson)

		val command = FindAndModify(
			questionCollection.name,
			selector,
			Update(modifier, true))

	  db.command(command)
	}

	def topicsBSON(): Future[Stream[BSONDocument]] = {
		Logger.debug("Retrieve distinct topics")

		val command = Aggregate(topicsCollection.name, Seq(
			Sort(Seq(Ascending("topic")))
		))
		db.command(command)
	}

	def topicBSON(topic: String): Future[BSONDocument] = {
		Logger.debug("Finding topic: " + topic)

		val command = Aggregate(topicsCollection.name, Seq(
			Match(BSONDocument("topic" -> topic))
		))

		val topics = db.command(command)

		topics.map{ l => if (l.isEmpty) BSONDocument() else l(0) }
	}

	def addTopic(course: String, term_year: String, q:String, topic: String): Future[Option[BSONDocument]] = {
		updateTopic(course, term_year, q, topic, "$addToSet")
	}

	def removeTopic(course: String, term_year: String, q:String, topic: String): Future[Option[BSONDocument]] = {
		updateTopic(course, term_year, q, topic, "$pull")
	}

	private def updateTopic(course: String, term_year: String, q:String, topic: String, opt: String): Future[Option[BSONDocument]] = {
		val (term, year) = getTermAndYear(term_year)

		val selector = BSONDocument(
			"course" -> course, "term" -> term,
			"year" -> year.toInt, "question" -> q)

		val modifier = BSONDocument(
			opt -> BSONDocument("topics" -> BSONString(topic)))

		val command = FindAndModify(
			questionCollection.name,
			selector,
			Update(modifier, true))

		db.command(command)
	}

	def updateUser(user: User): Future[Option[BSONDocument]] = {
		implicit val userWriter = User.UserWriter
		implicit val profileWriter = User.BasicProfileWriter
		user.identities.foreach( p => updateProfile(UserKey(p), p))
		updateProfile(user.userkey, user.main)
		val selector = BSONDocument("uid" -> user.userkey.key)
		val modifier = BSONDocument("$set" -> User.UserWriter.write(user))
		val command = FindAndModify(usersCollection.name, selector, Update(modifier, false), true)
		db.command(command)
	}

	def updateProfile(userKey: UserKey, profile: BasicProfile)(implicit writer: BSONDocumentWriter[BasicProfile]): Future[Option[BSONDocument]] = {
		val selector = BSONDocument( "uid" -> userKey.key)
		val modifier = BSONDocument("$set" -> writer.write(profile))
		val command = FindAndModify(profilesCollection.name, selector, Update(modifier, false), true)
		db.command(command)
	}
}

