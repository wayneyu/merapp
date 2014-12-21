import play.api.libs.json._

import scala.io.Source

val json_string = Source.fromFile("/home/wayneyu/mer/raw_database/json_data/MATH100/December_2013/Question_01_(a).json").getLines().mkString
val js = Json.parse(json_string)
val stmt = (js \ "statement").as[String]
val arr = Array(1,2,2)

val String = "What happens if you simply plug in -2 in the function? What does it mean? What can you do about that?"