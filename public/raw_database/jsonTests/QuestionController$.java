package controllers;

/*
 * Example using ReactiveMongo + Play JSON library.
 *
 * There are two approaches demonstrated in this controller:
 * - using JsObjects directly
 * - using case classes that can be turned into Json using Reads and Writes.
 *
 * This controller uses case classes and their associated Reads/Writes
 * to read or write JSON structures.
 *
 * Instead of using the default Collection implementation (which interacts with
 * BSON structures + BSONReader/BSONWriter), we use a specialized
 * implementation that works with JsObject + Reads/Writes.
 *
 * Of course, you can still use the default Collection implementation
 * (BSONCollection.) See ReactiveMongo examples to learn how to use it.
 */
object QuestionController extends Controller with MongoController {
  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("questions")

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //
  import play.api.data.Form
  import models._
  import models.JsonFormats._

  def create = Action.async {
    val q = Question("MATH101", 2012, "Winter",
      "this is a statement",
      List("this is a hint1","this is hint2"),
      List("this is a solution","this is a solution2")
    )

    // insert the question
    val futureResult = collection.insert(q)
    // when the insert is performed, send a OK 200 result
    futureResult.map(_ => Ok)
  }

  def createFromJson = Action.async(parse.json) { request =>
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
    request.body.validate[Question].map { q =>
      // `question` is an instance of the case class `models.Question`
      collection.insert(q).map { lastError =>
        Logger.debug("Successfully inserted with LastError: $lastError")
        Created
      }
    }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findByYear(year: String) = Action.async {
    // let's do our query
    val cursor: Cursor[Question] = collection.
      find(Json.obj("year" -> year)).
      // perform the query and get a cursor of JsObject
      cursor[Question]

    // gather all the JsObjects in a list
    val futureQuestionsList: Future[List[Question]] = cursor.collect[List]()

    // everything's ok! Let's reply with the array
    futureQuestionsList.map { questions =>
      Ok(questions.toString)
    }
  }

}
