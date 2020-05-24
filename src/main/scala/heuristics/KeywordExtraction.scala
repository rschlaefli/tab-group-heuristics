package heuristics

import com.typesafe.scalalogging.LazyLogging
import smile.nlp._

object KeywordExtraction extends LazyLogging {
  def apply(input: String): List[String] = {
    val normalizedInput = input.replaceAll("· | \\|", " ").normalize
    val tokenizedInput = normalizedInput.words("google")
    val frequencyMap = tokenizedInput.mkString(" ").bag("none")
    val sortedFrequencyMap = frequencyMap.toSeq.sortBy(_._2).reverse

    logger.debug(s"> Sorted frequency map ${sortedFrequencyMap.toString}")

    sortedFrequencyMap.take(4).map(_._1).toList
  }
}
