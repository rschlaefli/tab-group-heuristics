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
  var acceptedGroups: List[Int] = List()
  var acceptedTabs: List[Int] = List()
  var discardedGroups: List[Int] = List()
  var discardedTabs: List[Int] = List()
  var tabAge: List[Double] = List()
  var tabStaleness: List[Double] = List()

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
    acceptedGroups = acceptedGroups.appended(dataPoint.acceptedGroups)
    acceptedTabs = acceptedTabs.appended(dataPoint.acceptedTabs)
    discardedGroups = discardedGroups.appended(dataPoint.discardedGroups)
    discardedTabs = discardedTabs.appended(dataPoint.discardedTabs)
    tabAge = tabAge.appended(dataPoint.averageTabAge)
    tabStaleness = tabStaleness.appended(dataPoint.averageTabStaleDuration)
    return this
  }

  def aggregated: StatisticsOutput =
    StatisticsOutput(
      round(mean(numCurrentTabs.toArray), 2),
      round(mean(openTabsGrouped.toArray), 2),
      round(mean(openTabsUngrouped.toArray), 2),
      tabSwitchWithinGroups.sum,
      tabSwitchBetweenGroups.sum,
      tabSwitchFromGroup.sum,
      tabSwitchToGroup.sum,
      tabSwitchUngrouped.sum,
      acceptedGroups.sum,
      acceptedTabs.sum,
      discardedGroups.sum,
      discardedTabs.sum,
      round(mean(tabAge.toArray), 2),
      round(mean(tabStaleness.toArray), 2)
    )

  override def toString(): String = {
    s"StatisticsData(" +
      s"$numCurrentTabs, $openTabsGrouped, $openTabsUngrouped, " +
      s"$tabSwitchWithinGroups, $tabSwitchBetweenGroups, $tabSwitchFromGroup, $tabSwitchToGroup, $tabSwitchUngrouped, " +
      s"$acceptedGroups, $acceptedTabs, $discardedGroups, $discardedTabs, " +
      s"$tabAge, $tabStaleness" +
      s")"
  }
}
