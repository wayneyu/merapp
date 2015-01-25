package controllers

// Reactive Mongo plugin, including the JSON-specialized collection

import models.{Question, Topic}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson.{BSONDocument, _}
import reactivemongo.core.commands.{Aggregate, Ascending, Match, Sort}

import scala.concurrent.Future

/**
 * Created by wayneyu on 1/8/15.
 */
object TopicController extends Controller with MongoController {

  val collection = db[BSONCollection]("topics")

  def topic(topic: String) = Action.async {
    val topicResult = topicBSON(topic)
    val questionsResult = QuestionController.questionsForTopic(topic)

    val res = for {
      tr <- topicResult
      qr <- questionsResult
    } yield (tr, qr)

    res.map{
      case (tr, qr) => Ok(views.html.topic(
        tr.as[Topic], qr.map( _.as[Question] )
      ))
    }

  }

  def topics() = Action.async {
    val alltopics = topicsBSON()

    alltopics.map{
      l => Ok(views.html.topics(
        l.map { t => t.as[Topic] }
      ))
    }
  }

  def topicsBSON(): Future[List[BSONDocument]] = {
    Logger.info("Retrieve distinct topics")

    val command = Aggregate(collection.name, Seq(
      Sort(Seq(Ascending("topic")))
    ))

    val topics = db.command(command)

    topics.map{ _.toList }
  }

  def topicBSON(topic: String): Future[BSONDocument] = {
    Logger.info("Finding topic: " + topic)

    val command = Aggregate(collection.name, Seq(
      Match(BSONDocument("topic" -> topic))
    ))

    val topics = db.command(command)

    topics.map{ l => if (l.isEmpty) BSONDocument() else l(0) }
  }
}
