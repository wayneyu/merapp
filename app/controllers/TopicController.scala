package controllers

import models.{Question, Topic}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{AnyContent, Action, Controller}
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.BSONFormats.BSONArrayFormat
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.{Aggregate, Ascending, Match, Sort}
import service._
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

/**
 * Created by wayneyu on 1/8/15.
 */

object TopicController extends ServiceComponent {

  def topic(topic: String) = UserAwaredAction.async { implicit context =>
    val topicResult = MongoDAO.topicBSON(topic)
    val questionsResult = MongoDAO.questionsForTopic(topic)

    val res = for {
      tr <- topicResult
      qr <- questionsResult
    } yield (tr, qr)

    res.map{
      case (tr, qr) => Ok(views.html.topic(
        tr.as[Topic], qr.map( _.as[Question] ).toList
      ))
    }

  }

  def topics() = UserAwaredAction.async { implicit context =>
    val alltopics = MongoDAO.topicsBSON()

    alltopics.map{
      l => Ok(views.html.topics(
        l.map { t => t.as[Topic] }.toList
      ))
    }
  }

	def topicsSearch(searchTerm: String) = UserAwaredAction.async { implicit context =>
		val alltopics = MongoDAO.topicSearchBSON(searchTerm)

		alltopics.map { st =>
		   Ok(BSONArrayFormat.writes(BSONArray(
			  st)))
		}
	}

  def displayAllTopics() = UserAwaredAction.async { implicit context =>
    val alltopics = MongoDAO.topicParentAndChildren()

    alltopics.map { st =>
      Ok(BSONArrayFormat.writes(BSONArray(
      st)))

    }
  }

}