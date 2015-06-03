package models

import reactivemongo.bson._

/**
 * Created by berny on 02/06/15.
 */
case class MultipleChoice(userID: String,
                     questionID: String,
                     time: Long,
                     indexArray: List[Int]) {
}

object MultipleChoice {

  implicit object MultipleChoiceReader extends BSONDocumentReader[MultipleChoice] {
    def read(doc: BSONDocument): MultipleChoice = {
      MultipleChoice(
        doc.getAs[String]("userID").getOrElse(""),
        doc.getAs[String]("questionID").getOrElse(""),
        doc.getAs[Long]("time").getOrElse(0L),
        doc.getAs[List[Int]]("indexArray").getOrElse(List())
      )
    }
  }

  implicit object MultipleChoiceWriter extends BSONDocumentWriter[MultipleChoice] {
    def write(mc: MultipleChoice): BSONDocument = BSONDocument( Seq(
      "userID" -> BSONString(mc.userID),
      "questionID" -> BSONString(mc.questionID),
      "time" -> BSONDateTime(mc.time),
      "indexArray" -> BSONArray(mc.indexArray.map{ BSONInteger })
    ))
  }

  val empty = MultipleChoice("", "", 0L, List())

}
