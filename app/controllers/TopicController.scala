package controllers

import models.{Question, Topic}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Action, Controller}
import play.modules.reactivemongo.MongoController
import reactivemongo.api.collections.default.BSONCollection
import reactivemongo.bson._
import reactivemongo.core.commands.{Aggregate, Ascending, Match, Sort}
import service.{MongoDAO, ServiceComponent, User}
import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

/**
 * Created by wayneyu on 1/8/15.
 */

trait TopicController extends securesocial.core.SecureSocial[User] with ServiceComponent with MongoController {

  override implicit val env = AuthRuntimeEnvironment

  val collection = db[BSONCollection]("topics")

  def topic(topic: String) = UserAwareAction.async { implicit request =>
	  implicit val user = request.user
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

  def topics() = UserAwareAction.async { implicit request =>
	  implicit val user = request.user
    val alltopics = MongoDAO.topicsBSON()

    alltopics.map{
      l => Ok(views.html.topics(
        l.map { t => t.as[Topic] }.toList
      ))
    }
  }

}

object TopicController extends TopicController