package models

import reactivemongo.bson._

/**
 * Created by wayneyu on 2/9/15.
 */
case class Vote(userid: String,
                 qid: String,
                 timestamp: Long,
                 rating: Int) {

}

object Vote {

	implicit object VoteReader extends BSONDocumentReader[Vote] {
		def read(doc: BSONDocument): Vote = {
			Vote(
				doc.getAs[String]("userid").getOrElse(""),
				doc.getAs[String]("qid").getOrElse(""),
				doc.getAs[Long]("timestamp").getOrElse(0L),
				doc.getAs[Int]("rating").getOrElse(-1)
			)
		}
	}

	implicit object VoteWriter extends BSONDocumentWriter[Vote] {
		def write(v: Vote): BSONDocument = BSONDocument( Seq(
			"userid" -> BSONString(v.userid),
			"qid" -> BSONString(v.qid),
			"timestamp" -> BSONDateTime(v.timestamp),
			"rating" -> BSONInteger(v.rating)
		))
	}

	val empty = Vote("", "", 0L, -1);

}
