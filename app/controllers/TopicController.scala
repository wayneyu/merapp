package controllers

// Reactive Mongo plugin, including the JSON-specialized collection

import models.Topic

import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.api.Cursor
import reactivemongo.bson.BSONDocument
import reactivemongo.core.commands.{Match, Ascending, Sort, Aggregate}

import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import scala.concurrent.Future

/**
 * Created by wayneyu on 1/8/15.
 */
object TopicController extends Controller with MongoController {

  val collection = db[BSONCollection]("topics")

  def topic(topic: String) = Action.async {
    val res = topicBSON(topic)

    res.map{
      d => Ok(views.html.topic(
        d.as[Topic]
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

    topics.map{ _(0) }
  }
}
