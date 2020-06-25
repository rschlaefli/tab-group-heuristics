package groupnaming

import com.typesafe.scalalogging.LazyLogging
import smile.nlp._
import tabswitches.TabMeta
import smile.nlp.stemmer.Stemmer

case class KeywordParams(
    tokenizer: String = "comprehensive",
    bagging: String = "none",
    topK: Int = 4,
    stemmer: Option[Stemmer] = None
) extends NameGeneratorParameters

object BasicKeywords extends LazyLogging with NameGenerator[KeywordParams] {

  def apply(tabSet: Set[TabMeta]): (String, Set[TabMeta]) =
    apply(tabSet, KeywordParams())

  def apply(
      tabSet: Set[TabMeta],
      params: KeywordParams
  ): (String, Set[TabMeta]) = {

    val allTitles = tabSet
      .map(deriveTabKeywordString)
      .mkString(" ")

    val keywords = extractKeywordsFromString(allTitles, params)

    (keywords.mkString(" "), tabSet)
  }

  def extractKeywordsFromString(
      input: String,
      params: KeywordParams
  ): List[String] = {
    val frequencyMap = input.bag(params.bagging, params.stemmer)
    val sortedFrequencyMap = frequencyMap.toSeq.sortBy(_._2).reverse
    sortedFrequencyMap.take(params.topK).map(_._1).toList
  }

  def deriveTabKeywordString(tab: TabMeta) = {
    s"${tab.title} ${tab.url.split("/").drop(2).mkString(" ")}"
  }

}
