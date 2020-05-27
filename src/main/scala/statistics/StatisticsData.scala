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
    this.numCurrentTabs.appended(dataPoint.currentlyOpenTabs)
    this.openTabsGrouped.appended(dataPoint.openTabsGrouped)
    this.openTabsUngrouped.appended(dataPoint.openTabsUngrouped)
    this.tabSwitchWithinGroups.appended(dataPoint.switchesWithinGroups)
    this.tabSwitchBetweenGroups.appended(dataPoint.switchesBetweenGroups)
    this.tabSwitchFromGroup.appended(dataPoint.switchesFromGroups)
    this.tabSwitchToGroup.appended(dataPoint.switchesToGroups)
    this.tabSwitchUngrouped.appended(dataPoint.switchesOutsideGroups)
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

}
