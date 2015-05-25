package helpers

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

/**
 * Created by berny on 23/05/15.
 */

package object helpers {

  def course_and_term_year_and_number_from_ID(ID: String): (String, String, String) = {
    val pattern = """(.*)\+(.*)\+(.*)_(\d\d\d\d)\+(.*)""".r
    val res = ID match {
      case pattern(school, course, term, year, number) => (course, term + "_" + year, number)
    }
    res
  }

  def course_and_term_and_year_and_number_from_ID(ID: String): (String, String, Int, String) = {
    val (course, term_year, number) = course_and_term_year_and_number_from_ID(ID)
    (course, term_year.split("_")(0), term_year.split("_")(1).toInt, number)
  }

  def number_human_from_number(number: String): String = {
    number.replaceAll("0(\\d)", "\\1").replace("_", " ")
  }
}
