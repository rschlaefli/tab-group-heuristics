package groupnaming

import com.typesafe.scalalogging.LazyLogging
import smile.nlp._
import tabswitches.TabMeta

object BasicKeywords extends LazyLogging with NameGenerator {

  def apply(tabSet: Set[TabMeta]): (String, Set[TabMeta]) = {

    val allTitles = tabSet
      .map(deriveTabKeywordString)
      .mkString(" ")

    val keywords = extractKeywordsFromString(allTitles)

    (keywords.mkString(" "), tabSet)
  }

  def extractKeywordsFromString(input: String): List[String] = {
    val normalizedInput = input.normalize
    val tokenizedInput = normalizedInput.words("google")
    val frequencyMap = tokenizedInput.mkString(" ").bag("none")
    val sortedFrequencyMap = frequencyMap.toSeq.sortBy(_._2).reverse

    sortedFrequencyMap.take(4).map(_._1).toList
  }

  def deriveTabKeywordString(tab: TabMeta) = {
    s"${tab.title} ${tab.url.split("/").drop(2).mkString(" ")}"
  }

}
