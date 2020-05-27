package statistics

import smile.math.MathEx._

class StatisticsData() {
  var numCurrentTabs: List[Int] = List()
  var openTabsGrouped: List[Int] = List()
  var openTabsUngrouped: List[Int] = List()
  var tabSwitchWithinGroups: List[Int] = List()
  var tabSwitchBetweenGroups: List[Int] = List()
  var tabSwitchFromGroup: List[Int] = List()
  var tabSwitchToGroup: List[Int] = List()
  var tabSwitchUngrouped: List[Int] = List()

  def withDataPoint(dataPoint: DataPoint): StatisticsData = {
    numCurrentTabs = numCurrentTabs.appended(dataPoint.currentlyOpenTabs)
    openTabsGrouped = openTabsGrouped.appended(dataPoint.openTabsGrouped)
    openTabsUngrouped = openTabsUngrouped.appended(dataPoint.openTabsUngrouped)
    tabSwitchWithinGroups =
      tabSwitchWithinGroups.appended(dataPoint.switchesWithinGroups)
    tabSwitchBetweenGroups =
      tabSwitchBetweenGroups.appended(dataPoint.switchesBetweenGroups)
    tabSwitchFromGroup =
      tabSwitchFromGroup.appended(dataPoint.switchesFromGroups)
    tabSwitchToGroup = tabSwitchToGroup.appended(dataPoint.switchesToGroups)
    tabSwitchUngrouped =
      tabSwitchUngrouped.appended(dataPoint.switchesOutsideGroups)
    return this
  }

  def aggregated: StatisticsOutput =
    StatisticsOutput(
      median(numCurrentTabs.toArray),
      median(openTabsGrouped.toArray),
      median(openTabsUngrouped.toArray),
      tabSwitchWithinGroups.sum,
      tabSwitchBetweenGroups.sum,
      tabSwitchFromGroup.sum,
      tabSwitchToGroup.sum,
      tabSwitchUngrouped.sum
    )

  override def toString(): String = {
    s"StatisticsData($numCurrentTabs, $openTabsGrouped, $openTabsUngrouped, $tabSwitchWithinGroups " +
      s"$tabSwitchBetweenGroups, $tabSwitchFromGroup, $tabSwitchToGroup, $tabSwitchUngrouped)"
  }
}
