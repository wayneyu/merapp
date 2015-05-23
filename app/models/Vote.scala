package models

import reactivemongo.bson._

/**
 * Created by wayneyu on 2/9/15.
 */
case class Vote(userID: String,
                 questionID: String,
                 time: Long,
                 rating: Int) {

}

object Vote {

	implicit object VoteReader extends BSONDocumentReader[Vote] {
		def read(doc: BSONDocument): Vote = {
			Vote(
				doc.getAs[String]("userID").getOrElse(""),
				doc.getAs[String]("questionID").getOrElse(""),
				doc.getAs[Long]("time").getOrElse(0L),
				doc.getAs[Int]("rating").getOrElse(-1)
			)
		}
	}

	implicit object VoteWriter extends BSONDocumentWriter[Vote] {
		def write(v: Vote): BSONDocument = BSONDocument( Seq(
			"userID" -> BSONString(v.userID),
			"questionID" -> BSONString(v.questionID),
			"time" -> BSONDateTime(v.time),
			"rating" -> BSONInteger(v.rating)
		))
	}

	val empty = Vote("", "", 0L, -1)

}
