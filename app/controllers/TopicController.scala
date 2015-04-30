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


import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import scala.util.parsing.json.JSONObject

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

  def displayAllTopics() = Action {
    // Creates JSON for topics bubble chart. Format is
//    {
//      "name": "MER Topics",
//      "children":[
//      {
//        "name" : "Parent1",
//        "children" : [
//        {"name": "subtopic1", "size": 3, "url": "/topics/subtopic1"},
//        {"name": "subtopic2", "size": 5, "url": "/topics/subtopic2"}
//        ]
//      },
//      {
//        "name" : "Parent2",
//        "children" : [
//        {"name": "subtopic3", "size": 15, "url": "/topics/subtopic3"}
//        ]
//      }
//      ]
//    }
//    size is the number of questions on the particular subtopic
    
    val topicsParentandChildren = MongoDAO.topicParentAndChildren()

    val stream_parent_sub = topicsParentandChildren.map { st =>
      for {
        s <- st
        parent <- s.getAs[String]("_id")
        childList <- s.getAs[List[String]]("subtopics")
      } yield (parent, childList)
    }

    // obtain helper function
    val countPerTopic = getCountPerTopic()

    var array_of_parents: BSONArray = BSONArray.empty

    // TODO: Remove Await
    Await.result(stream_parent_sub map (_.toList), 5.seconds) foreach {
      case (parent, sublist) => {
        var array_of_children: BSONArray = BSONArray.empty
        sublist foreach {
          case (topic) => array_of_children = array_of_children add BSONDocument("name" -> topic, "size" -> countPerTopic(topic), "url" -> BSONString("/topics/" + topic))
        }
        array_of_parents = array_of_parents add BSONDocument("name" -> parent, "children" -> array_of_children)
      }
    }

    val res: BSONDocument = BSONDocument("name" -> "MER Topics", "children" -> array_of_parents)

    Ok(BSONArrayFormat.writes(BSONArray(res)))
  }


  def getCountPerTopic(): Map[String, Int] = {
    // Returns a map as lookup table for number of questions on each topic.

    val questionsPerTopic = MongoDAO.questionsPerTopic()
    var myMap: Map[String, Int] = Map.empty

    val stream_topic_count = questionsPerTopic.map { st =>
      for {
        s <- st
        topic <- s.getAs[String]("_id")
        num_questions <- s.getAs[Int]("num_questions")
      } yield (topic, num_questions)
    }

    // TODO: Remove await
    Await.result(stream_topic_count map (_.toList), 5.seconds) foreach {
      case(topic, num_questions) => myMap = myMap ++ Map(topic -> num_questions)
    }

    myMap
  }


  def displayTopicsCount() = Action {
    val CountPerTopic = getCountPerTopic()
    Ok(scala.util.parsing.json.JSONObject(CountPerTopic).toString())
  }



}