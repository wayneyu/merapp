package models

/**
 * Created by wayneyu on 12/11/14.
 */
case class SearchResult(q: Question) {

  def apply = q.url

}
