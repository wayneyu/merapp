package object assets {

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

  def term_and_year_from_term_year(term_year: String): (String, Int) = {
    val term_and_year = term_year.split("_")
    val term = term_and_year(0)
    val year = term_and_year(1).toInt
    (term, year)
  }

  def term_year_from_term_and_year(term: String, year: String): String = {
    s"$term\\_$year"
  }

  def term_year_from_term_and_year(term: String, year: Int): String = {
    term_year_from_term_and_year(term, year.toString)
  }

  def ID_from_course_and_term_year_and_number(course: String, term_year: String, number: String): String = {
    val school = "UBC"
    s"$school+$course+$term_year+$number"
  }

}
