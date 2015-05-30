package service

import models._
import play.api._
//import play.api.libs.json._
import play.api.mvc._
//import play.modules.reactivemongo.json.BSONFormats._
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import securesocial.core.BasicProfile

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

import reactivemongo.api._
import reactivemongo.core.commands._

import play.modules.reactivemongo.MongoController

import scala.util.{Failure, Success}

/**
 * Created by wayneyu on 01/27/15.
 */

object MongoDAO extends Controller with MongoController {

	val questionCollection = db[BSONCollection]("questions")
	val topicsCollection = db[BSONCollection]("topics")
	val usersCollection = db[BSONCollection]("users")
	val profilesCollection = db[BSONCollection]("profiles")
	val votesCollection = db[BSONCollection]("votes")



	// FINDING THINGS

	def questionQuery(ID: String): Future[List[BSONDocument]] = {
		val cursor: Cursor[BSONDocument] = questionCollection
			.find(BSONDocument("ID" -> ID))
			.cursor[BSONDocument]
		// gather all the JsObjects in a list
		cursor.collect[List]()
	}

	def questionQuery(course: String, term_year: String, number: String): Future[List[BSONDocument]] = {
		val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
		questionQuery(ID)
	}

	def questionInJson(course: String, term_year: String, number: String) = Action.async {
		questionQuery(course, term_year, number)
			.map(l =>
			  Ok(BSONArray.pretty(BSONArray(l)))
			)
	}


	// FINDING COURSES

	def distinctCourses(MatchCondition: BSONDocument = BSONDocument()): Future[Stream[BSONDocument]] = {
		if (MatchCondition.isEmpty) {
			val command = Aggregate(questionCollection.name, Seq(
				GroupField("course")("course" -> First("course")),
				Sort(Seq(Ascending("course"))),
				Project("_id" -> BSONInteger(0), "course" -> BSONInteger(1))
			))
			db.command(command)
		} else {
			val command = Aggregate(questionCollection.name, Seq(
				Match(MatchCondition),
				GroupField("course")("course" -> First("course")),
				Sort(Seq(Ascending("course"))),
				Project("_id" -> BSONInteger(0), "course" -> BSONInteger(1))
			))
			db.command(command)
		}
	}

	def distinctCourses(year: Int): Future[Stream[BSONDocument]] = {
		distinctCourses(MatchCondition = BSONDocument("year" -> year))
	}

	def distinctCourses(year: Int, term: String): Future[Stream[BSONDocument]] = {
		distinctCourses(MatchCondition = BSONDocument("year" -> year, "term" -> term))
	}



  // FINDIND YEARS

	def distinctYears(MatchCondition: BSONDocument = BSONDocument()): Future[Stream[BSONDocument]] = {
		if (MatchCondition.isEmpty) {
			val command = Aggregate(questionCollection.name, Seq(
				GroupField("year")("year" -> First("year")),
				Sort(Seq(Descending("year"))),
				Project("_id" -> BSONInteger(0), "year" -> BSONInteger(1))
			))
			db.command(command)
		} else {
			val command = Aggregate(questionCollection.name, Seq(
  			Match(MatchCondition),
				GroupField("year")("year" -> First("year")),
				Sort(Seq(Descending("year"))),
				Project("_id" -> BSONInteger(0), "year" -> BSONInteger(1))
			))
			db.command(command)
		}
	}

	def distinctYears(course: String): Future[Stream[BSONDocument]] = {
		distinctYears(MatchCondition = BSONDocument("course" -> course))
	}

	def distinctYears(course: String, term: String): Future[Stream[BSONDocument]] = {
		distinctYears(MatchCondition = BSONDocument("course" -> course, "term" -> term ))
	}


  // FINDING TERMS

	def distinctTerms(MatchCondition: BSONDocument = BSONDocument()): Future[Stream[BSONDocument]] = {
		if (MatchCondition.isEmpty) {
			val command = Aggregate(questionCollection.name, Seq(
				GroupField("term")("term" -> First("term")),
				Sort(Seq(Ascending("term"))),
				Project("_id" -> BSONInteger(0), "term" -> BSONInteger(1))
			))
			db.command(command)
		} else {
			val command = Aggregate(questionCollection.name, Seq(
  			Match(MatchCondition),
				GroupField("term")("term" -> First("term")),
				Sort(Seq(Ascending("term"))),
				Project("_id" -> BSONInteger(0), "term" -> BSONInteger(1))
			))
			db.command(command)
		}
	}

	def distinctTerms(course: String): Future[Stream[BSONDocument]] = {
		distinctTerms(MatchCondition = BSONDocument("course" -> course))
	}

	def distinctTerms(course: String, year: String): Future[Stream[BSONDocument]] = {
		distinctTerms(MatchCondition = BSONDocument("course" -> course, "year" -> year))
	}


  // FINDING EXAMS

  def distinctExams(): Future[Stream[BSONDocument]] = {
    val command = Aggregate(questionCollection.name, Seq(
      GroupMulti("course" -> "course", "year" -> "year", "term" -> "term")
        ("course" -> First("course"), "year" -> First("year"), "term" -> First("term")),
      Project("_id" -> BSONInteger(0), "course" -> BSONInteger(1),
        "year" -> BSONInteger(1), "term" -> BSONInteger(1), "term_year" -> BSONInteger(1))
    ))

    db.command(command)
  }

  def examsForCourse(course: String): Future[Stream[BSONDocument]] = {
    val command = Aggregate(questionCollection.name, Seq(
      Match(BSONDocument("course" -> course)),
      GroupMulti("year" -> "year", "term" -> "term")("year" -> First("year"), "term" -> First("term")),
      Sort(Seq(Descending("year"), Ascending("term"))),
      Project("_id"->BSONInteger(0), "year" -> BSONInteger(1), "term" -> BSONInteger(1))
    ))

    db.command(command)
  }


  // HELPERS AND MISC

	def distinctContributors(): Future[Stream[BSONDocument]] = {
		// db.questions.distinct("contributors")
		val command = Aggregate(questionCollection.name, Seq(
			Unwind("contributors"),
			GroupField("contributors")("contributors" -> First("contributors")),
			Sort(Seq(Ascending("contributors"))),
			Project("_id" -> BSONInteger(0), "contributors" -> BSONInteger(1))
		))
		db.command(command)
	}

	def distinct_question_numbers_for_course_and_term_year(course: String, term_year: String): Future[Stream[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course, "term_year" -> term_year)),
			GroupField("number")("number" -> First("number")),
			Sort(Seq(Ascending("number"))),
			Project("_id" -> BSONInteger(0), "number" -> BSONInteger(1))
		))
		db.command(command)
	}

	def list_question_BSONDocument_for_course_and_term_year(course: String, term_year: String): Future[List[BSONDocument]] = {
		val command = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("course" -> course, "term_year" -> term_year)),
			Sort(Seq(Ascending("number")))
		))
		db.command(command).map{_.toList}
	}


  // TEXT SEARCH

	def searchById(id: String): Future[List[BSONDocument]] = {
		Logger.debug("search question with id = " + id)
		val cursor = questionCollection.
			// find with case-insensitive and use . to match any chars options
			find(BSONDocument("_id" -> BSONObjectID(id))).
			cursor[BSONDocument]

		cursor.collect[List]()
	}

	def searchByKeywordsQuery(searchString: String): Future[List[BSONDocument]] = {
		// Search for keywords in statement, solutions, hints, answers and topics field of Questions collection
		// Results are returned as an array of BSONDocument
		Logger.debug("searching for " + searchString)

		val searchCommand = Aggregate(questionCollection.name, Seq(
			Match(BSONDocument("$text" -> BSONDocument("$search" -> searchString))),
			Project("_id" -> BSONInteger(0), "textScore" -> BSONDocument("$meta" -> "textScore"), "course" -> BSONInteger(1),
				"year" -> BSONInteger(1), "term" -> BSONInteger(1), "number" -> BSONInteger(1),
				"statement_html" -> BSONInteger(1), "ID" -> BSONInteger(1)),
			Sort(Seq(Descending("textScore")))
		))
		val result = db.command(searchCommand)

		result.map { _.toList }
	}


  // HELPERS

//	def bson2url(doc: BSONDocument) = Json.toJson(doc).as[Question].url

//	def toJsArray(jsoList: List[JsValue]): JsArray = {
//		jsoList.foldLeft(JsArray())((acc, x) => acc ++ Json.arr(x))
//	}

  private def updateArray(ID: String, whichArray: String, newValue: String, opt: String): Future[Option[BSONDocument]] = {
    // A helper function
    // Updates whichArray in db.questions with newValue
    val selector = BSONDocument("ID" -> ID)

    val modifier = BSONDocument(
      opt -> BSONDocument(whichArray -> BSONString(newValue)))

    val command = FindAndModify(
      questionCollection.name,
      selector,
      Update(modifier, fetchNewObject = true),
      upsert = false)

    db.command(command)
  }

  private def updateArray(course: String, term_year: String, number: String, whichArray: String, newValue: String, opt: String): Future[Option[BSONDocument]] = {
    val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
    updateArray(ID, whichArray, newValue, opt)
  }



  // TOPICS

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

  def topicsBSON(): Future[Stream[BSONDocument]] = {
    val command = Aggregate(topicsCollection.name, Seq(
      Sort(Seq(Ascending("topic")))
    ))
    db.command(command)
  }

  def topicBSON(topic: String): Future[BSONDocument] = {
    val command = Aggregate(topicsCollection.name, Seq(
      Match(BSONDocument("topic" -> topic))
    ))
    val topics = db.command(command)

    topics.map{ l => if (l.isEmpty) BSONDocument() else l.head }
  }

  def topicSearchBSON(searchString: String): Future[Stream[BSONDocument]] = {
    // Finds all topics that contain searchString in their name
    val command = Aggregate(topicsCollection.name, Seq(
      Match(BSONDocument("topic" -> BSONRegex(searchString, "i"))),
      Project("_id" -> BSONInteger(0), "topic" -> BSONInteger(1)),
      Sort(Seq(Ascending("topic")))
    ))
    db.command(command)
  }


  def questionsPerTopic(): Future[Stream[BSONDocument]] = {
    // To get the number of questions per topic
    // db.questions.aggregate([{"$unwind": "$topics"}, {"$group": {"_id" : "$topics", total: {$sum: 1}}}])
    val command = Aggregate(questionCollection.name, Seq(
      Unwind("topics"),
      GroupField("topics")(("num_questions", SumValue(1)))
    ))
    db.command(command)
  }

  def topicParentAndChildren(): Future[Stream[BSONDocument]] = {
    // To get all parent topics with children:
    // db.topics.aggregate([{"$group": {"_id" : "$parent", subtopics: {$addToSet: "$topic"}}}])
    val command = Aggregate(topicsCollection.name, Seq(
      GroupField("parent")("subtopics" -> AddToSet("topic"))
    ))
    db.command(command)
  }


  // MODIFY TOPICS

  def addTopic(ID: String, topic: String): Future[Option[BSONArray]] = {
    for{
      a <- updateArray(ID, "topics", topic, "$addToSet")
      b <- upsertToTopics(topic)
    } yield a.flatMap{_.getAs[BSONArray]("topics")}
  }

  def addTopic(course: String, term_year: String, number: String, topic: String): Future[Option[BSONArray]] = {
    val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
    addTopic(ID, topic)
  }

  def removeTopic(ID: String, topic: String): Future[Option[BSONArray]] = {
    for {
      a <- updateArray(ID, "topics", topic, "$pull")
      b <- removeFromTopics(topic)
    } yield a.flatMap{_.getAs[BSONArray]("topics")}
  }

  def removeTopic(course: String, term_year: String, number: String, topic: String): Future[Option[BSONArray]] = {
    val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
    removeTopic(ID, topic)
  }

  private def upsertToTopics(topic: String): Future[Option[BSONDocument]] = {
    val selector = BSONDocument("topic" -> topic)
    val modifier = BSONDocument("$setOnInsert" -> BSON.write(Topic(topic = topic)))
    val command = FindAndModify(topicsCollection.name, selector, Update(modifier, fetchNewObject = false), upsert = true)
    db.command(command)
  }

  private def removeFromTopics(topic: String): Future[Option[BSONDocument]] = {
    val selector = BSONDocument("content" -> BSONArray(), "url" -> "", "parent" -> "", "topic" -> topic)
    val command = FindAndModify(topicsCollection.name, selector, Remove, upsert = false)
    db.command(command)
  }


  // MODIFY CONTENT

  def questionFindAndModify(ID: String, bson: BSONDocument): Future[Option[BSONDocument]] = {
    val selector = BSONDocument("ID" -> ID)

    val modifier = BSONDocument(
      "$set" -> bson)

    val command = FindAndModify(
      questionCollection.name,
      selector,
      Update(modifier, fetchNewObject=true))

    db.command(command)
  }

	def questionFindAndModify(course: String, term_year: String, number: String, bson: BSONDocument): Future[Option[BSONDocument]] = {
    val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
    questionFindAndModify(ID, bson)
	}





  // QUALITY FLAGS AND PEER REVIEW SYSTEM

  def updateQualityFlag(ID: String, newQualityFlag: String): Future[Option[BSONArray]] = {
    // Adds newQualityFlag to "flags" and removes all other quality flags of the same type (Statement, Hint or Solution)
    def flags_to_remove(): List[String] = {
      // Returns all quality flags of the same type (Question Statement (Q), Hint(H) or Solution) as newQualityFlag,
      // except newQualityFlag itself.
      // Because adding and removing is a future, so the order is not guaranteed,
      // we have to make sure to exclude the newQualityFlag from the list of flags to remove.
      // Otherwise adding might be performed first and newQualityFlag is removed immediately after.

      // Quality flags have the form AB, where A indicates the current_quality and B is either Q (Question Statement),
      // H (Hint) or S (Solution).
      // eg RS means that Review is needed for the Solution

      val QStatement_or_Hint_or_Solution = newQualityFlag takeRight 1
      val current_quality = newQualityFlag slice (0, newQualityFlag.length-1)
      // C = Content needs to be added.
      // R = Review please.
      // QB = Quality is bad/should be improved.
      // QG = Quality is good. Final approval.
      val qualityFlags = List("C", "R", "QB", "QG") diff List(current_quality)
      qualityFlags map (_ + QStatement_or_Hint_or_Solution)
    }

    flags_to_remove() map {
      updateArray(ID, "flags", _, "$pull")
    }

    for {
      a <- updateArray(ID, "flags", newQualityFlag, "$push")
    } yield a.flatMap{_.getAs[BSONArray]("flags")}
  }

	def updateQualityFlag(course: String, term_year: String, number: String, newQualityFlag: String): Future[Option[BSONArray]] = {
    val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
    updateQualityFlag(ID, newQualityFlag)
  }

  def flags_per_exam(): Future[Stream[BSONDocument]] = {
    val command = Aggregate(questionCollection.name, Seq(
      Unwind("flags"),
      GroupMulti("course" -> "course", "term" -> "term", "year" -> "year", "flag" -> "flags")(("num_questions", SumValue(1)))
    ))
    db.command(command)
  }

  def questionsWithFlag(flag: String): Future[Stream[BSONDocument]] = {
    val command = Aggregate(questionCollection.name, Seq(
      Match(BSONDocument("flags" -> flag))
    ))
    db.command(command)
  }

  def questionsWithFlagCount(flag: String): Future[Int] = {
    val command = Count(questionCollection.name,
      Some(BSONDocument("flags" -> flag))
    )
    db.command(command)
  }

  def numberOfGoodQualitySolutions(): Future[Int] = {
    // count the number of solutions with the flag 'QGS' (indicating good quality solution)
    questionsWithFlagCount("QGS")
  }



  // USERS AND THEIR PROFILE

	def updateUser(user: User): Future[Option[BSONDocument]] = {
		implicit val userWriter = User.UserWriter
		implicit val profileWriter = User.BasicProfileWriter
		user.identities.foreach( p => updateProfile(UserKey(p), p))
		updateProfile(user.userkey, user.main)
		val selector = BSONDocument("uid" -> user.userkey.key)
		val modifier = BSONDocument("$set" -> User.UserWriter.write(user))
		val command = FindAndModify(usersCollection.name, selector, Update(modifier, fetchNewObject = false), upsert = true)
		db.command(command)
	}

	def updateProfile(userKey: UserKey, profile: BasicProfile)(implicit writer: BSONDocumentWriter[BasicProfile]): Future[Option[BSONDocument]] = {
		val selector = BSONDocument( "uid" -> userKey.key)
		val modifier = BSONDocument("$set" -> writer.write(profile))
		val command = FindAndModify(profilesCollection.name, selector, Update(modifier, fetchNewObject = false), upsert = true)
		db.command(command)
	}


  // VOTING FOR QUESTION DIFFICULTY

  def insertVote(vote: Vote, ID: String): Future[Option[BSONDocument]] = {
    votesCollection.insert[Vote](vote)
    for {
      lv <- lastVote(vote)
      q <- questionQuery(ID)
      upd <- updateQuestionRating(q.head.as[Question], lv, vote)
    } yield upd
  }

	def insertVote(vote: Vote, course: String, term_year: String, number: String): Future[Option[BSONDocument]] = {
    val ID = assets.ID_from_course_and_term_year_and_number(course, term_year, number)
    insertVote(vote, ID)
	}

	def updateQuestionRating(q: Question, lastVote: Option[Vote], vote: Vote): Future[Option[BSONDocument]] = {
		val selector = BSONDocument("ID" -> q.ID)
		def command(numVoteInc: Int, newRating: Double)  = FindAndModify(
			questionCollection.name,
			selector,
			Update(BSONDocument(
				"$inc" -> BSONDocument("num_votes" -> BSONInteger(numVoteInc)),
				"$set" -> BSONDocument("rating" -> BSONDouble(newRating))
			), fetchNewObject = true))

		val oldTotalRating = q.num_votes * q.rating //rating = -1 is taken care of by num_votes = 0
		Logger.debug("oldTotalRating:  " + q.num_votes + " * " + q.rating + " = " + oldTotalRating)

		// Find the rating difference between newVote and lastVote.
		// If the user has voted before, lastOption.isDefined == true
		val res = lastVote match {
			case Some(v) =>
				if (!v.questionID.equals(vote.questionID) || !v.userID.equals(vote.userID))
					throw new IllegalArgumentException("updateQuestionRating, lastVote: " + lastVote + " vote: " + vote)
				db.command(command(0, (oldTotalRating + (vote.rating - v.rating))/q.num_votes))
			case None =>
				db.command(command(1, (oldTotalRating + vote.rating)/(q.num_votes + 1)))
		}

		res onComplete {
			case Success(qr) =>
				val q = qr.map(_.as[Question]).get
				Logger.debug("new rating: " + q.rating + " num_votes: " + q.num_votes)
			case Failure(exception) =>
		}

		res.map{ opt => opt.map{ doc =>
			BSONDocument("rating" -> doc.get("rating"), "num_votes" -> doc.get("num_votes"))
			}
		}
	}

	def lastVote(vote: Vote): Future[Option[Vote]] = {
		// get last vote from user
		val command = Aggregate(votesCollection.name, Seq(
			Match(BSONDocument("questionID" -> vote.questionID, "userID" -> vote.userID,
				"time" -> BSONDocument(Seq("$lt" -> BSONDateTime(vote.time)))
			)),
			Sort(Seq(Descending("time")))
		))

		val res = db.command(command)

		res onComplete {
			case Success(v) => Logger.debug("last vote: " + v.headOption.map(_.as[Vote].toString))
			case Failure(exception) =>
		}

		res.map{
			st => st.map{l => l.as[Vote]}.headOption
		}
	}

  def getRating(user: User, qid: String): Future[Stream[BSONDocument]] = {
    val command = Aggregate(votesCollection.name, Seq(
      Match(BSONDocument("userID" -> user.userkey.key, "questionID" -> qid)),
      Sort(Seq(Descending("time"))),
      Project("_id" -> BSONInteger(0), "rating" -> BSONInteger(1))
    ))
    db.command(command)
  }
}
