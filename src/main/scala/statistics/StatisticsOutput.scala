package statistics

import smile.math.MathEx._

case class StatisticsOutput(
    numCurrentTabs: Double,
    openTabsGrouped: Double,
    openTabsUngrouped: Double,
    tabSwitchWithinGroups: Int,
    tabSwitchBetweenGroups: Int,
    tabSwitchFromGroup: Int,
    tabSwitchToGroup: Int,
    tabSwitchUngrouped: Int,
    acceptedGroups: Int,
    acceptedTabs: Int,
    discardedGroups: Int,
    discardedTabs: Int,
    tabAge: Double,
    tabStaleness: Double
) {
  def asCsv: String =
    Seq(
      numCurrentTabs,
      openTabsGrouped,
      openTabsUngrouped,
      tabSwitchWithinGroups,
      tabSwitchBetweenGroups,
      tabSwitchFromGroup,
      tabSwitchToGroup,
      tabSwitchUngrouped,
      acceptedGroups,
      acceptedTabs,
      discardedGroups,
      discardedTabs,
      tabAge,
      tabStaleness
    ).mkString(";")

}

object StatisticsOutput {
  def apply(measurements: List[StatisticsMeasurement]): StatisticsOutput = {
    val List(
      currentlyOpenTabs: List[Double],
      openTabsUngrouped: List[Double],
      openTabsGrouped: List[Double],
      averageTabAge: List[Double],
      averageTabStaleDuration: List[Double],
      switchesWithinGroups: List[Double],
      switchesBetweenGroups: List[Double],
      switchesFromGroups: List[Double],
      switchesToGroups: List[Double],
      switchesOutsideGroups: List[Double],
      acceptedGroups: List[Double],
      acceptedTabs: List[Double],
      discardedGroups: List[Double],
      discardedTabs: List[Double]
    ) = measurements.map(_.asSeq).transpose

    StatisticsOutput(
      round(mean(currentlyOpenTabs.map(_.intValue()).toArray), 2),
      round(mean(openTabsGrouped.map(_.intValue()).toArray), 2),
      round(mean(openTabsUngrouped.map(_.intValue()).toArray), 2),
      switchesWithinGroups.map(_.intValue()).sum,
      switchesBetweenGroups.map(_.intValue()).sum,
      switchesFromGroups.map(_.intValue()).sum,
      switchesToGroups.map(_.intValue()).sum,
      switchesOutsideGroups.map(_.intValue()).sum,
      acceptedGroups.map(_.intValue()).sum,
      acceptedTabs.map(_.intValue()).sum,
      discardedGroups.map(_.intValue()).sum,
      discardedTabs.map(_.intValue()).sum,
      round(mean(averageTabAge.toArray), 2),
      round(mean(averageTabStaleDuration.toArray), 2)
    )

  }
}
