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
    tabStaleness: Double,
    discardedRating: Double,
    discardedWrong: Int,
    discardedOther: Int,
    switchTime: Double,
    shortSwitches: Int,
    avgCuratedGroups: Double,
    curatedGroupsOpened: Int,
    curatedGroupsClosed: Int,
    focusModeUsed: Int
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
      tabStaleness,
      discardedRating,
      discardedWrong,
      discardedOther,
      switchTime,
      shortSwitches,
      avgCuratedGroups,
      curatedGroupsOpened,
      curatedGroupsClosed,
      focusModeUsed
    ).mkString(";")

}

object StatisticsOutput {
  def apply(measurements: List[StatisticsMeasurement]): StatisticsOutput = {
    val transposed = measurements.map(_.asSeq).transpose

    StatisticsOutput(
      round(mean(transposed(0).map(_.intValue()).toArray), 2),
      round(mean(transposed(1).map(_.intValue()).toArray), 2),
      round(mean(transposed(2).map(_.intValue()).toArray), 2),
      transposed(3).map(_.intValue()).sum,
      transposed(4).map(_.intValue()).sum,
      transposed(5).map(_.intValue()).sum,
      transposed(6).map(_.intValue()).sum,
      transposed(7).map(_.intValue()).sum,
      transposed(8).map(_.intValue()).sum,
      transposed(9).map(_.intValue()).sum,
      transposed(10).map(_.intValue()).sum,
      transposed(11).map(_.intValue()).sum,
      round(mean(transposed(12).toArray), 2),
      round(mean(transposed(13).toArray), 2),
      round(mean(transposed(14).toArray), 2),
      transposed(15).map(_.intValue).sum,
      transposed(16).map(_.intValue).sum,
      round(mean(transposed(17).map(_.intValue).toArray), 2),
      transposed(18).map(_.intValue).sum,
      round(mean(transposed(19).map(_.intValue()).toArray), 2),
      transposed(20).map(_.intValue).sum,
      transposed(21).map(_.intValue).sum,
      transposed(22).map(_.intValue).sum
    )

  }
}
