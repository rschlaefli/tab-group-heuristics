package heuristics

import com.typesafe.scalalogging.LazyLogging
import smile.nlp._
import tabswitches.TabMeta

object KeywordExtraction extends LazyLogging {

  def apply(tabSet: Set[TabMeta]): List[String] = {
    logger.debug(s"> Extracting keywords from set of tabs ${tabSet.toString()}")

    val allTitles = tabSet
      .map(deriveTabKeywordString)
      .mkString(" ")
    logger.debug(s"> Combined all titles in tab cluster: $allTitles")

    val keywords = extractKeywordsFromString(allTitles)
    logger.debug(s"> Extracted keywords from tab cluster: $keywords")

    keywords
  }

  def extractKeywordsFromString(input: String): List[String] = {
    val normalizedInput = input.normalize
    val tokenizedInput = normalizedInput.words("google")
    val frequencyMap = tokenizedInput.mkString(" ").bag("none")
    val sortedFrequencyMap = frequencyMap.toSeq.sortBy(_._2).reverse

    logger.debug(s"> Sorted frequency map ${sortedFrequencyMap.toString}")

    sortedFrequencyMap.take(4).map(_._1).toList
  }

  def deriveTabKeywordString(tab: TabMeta) = {
    s"${tab.title} ${tab.url.split("/").drop(2).mkString(" ")}"
  }

}
