package communitydetection

import smile.math.MathEx._
import tabswitches.TabMeta

case class SummaryStatistics(min: Double, max: Double, mean: Double, sd: Double)

case class CliqueStatistics(
    averageWeight: Double = 0d,
    connectedness: Double = 0d,
    pageRank: Double = 0d
) {

  def score = averageWeight + connectedness + pageRank

  def asSeq = Array(averageWeight, connectedness, pageRank)

  def normalized(
      weightSummary: SummaryStatistics,
      connSummary: SummaryStatistics,
      prSummary: SummaryStatistics
  ): CliqueStatistics = {
    CliqueStatistics(
      CliqueStatistics
        .normalize(weightSummary.min, weightSummary.max, averageWeight),
      CliqueStatistics
        .normalize(connSummary.min, connSummary.max, connectedness),
      CliqueStatistics
        .normalize(prSummary.min, prSummary.max, pageRank)
    )
  }
}

object CliqueStatistics {

  def normalize(min: Double, max: Double, value: Double): Double = {
    if (min == max) return value
    (value - min) / (max - min)
  }

  def standardize(mean: Double, sd: Double, value: Double): Double = {
    if (sd == 0) return value
    (value - mean) / sd
  }

  def computeSummaryStatistics(
      groups: Array[(Set[TabMeta], CliqueStatistics)]
  ): Array[SummaryStatistics] = {
    groups
      .map(_._2.asSeq)
      .transpose
      .map(arr => (SummaryStatistics(min(arr), max(arr), mean(arr), sd(arr))))
  }

  def normalize(groups: Array[(Set[TabMeta], CliqueStatistics)]) = {
    val Array(weightSummary, connSummary, prSummary) =
      CliqueStatistics.computeSummaryStatistics(groups)

    groups.map(group =>
      (group._1, group._2.normalized(weightSummary, connSummary, prSummary))
    )
  }

}
