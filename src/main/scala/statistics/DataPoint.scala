package statistics

class DataPoint(
    val currentlyOpenTabs: Int,
    val openTabsUngrouped: Int,
    val openTabsGrouped: Int
) {

  var switchesWithinGroups: Int = 0
  var switchesBetweenGroups: Int = 0
  var switchesFromGroups: Int = 0
  var switchesToGroups: Int = 0
  var switchesOutsideGroups: Int = 0

  def updateSwitchStatistics(switchStatistics: SwitchStatistics): Unit = {
    switchesWithinGroups = switchStatistics.switchesWithinGroups
    switchesBetweenGroups = switchStatistics.switchesWithinGroups
    switchesFromGroups = switchStatistics.switchesFromGroups
    switchesToGroups = switchStatistics.switchesToGroups
    switchesOutsideGroups = switchStatistics.switchesOutsideGroups
  }

  override def toString(): String = {
    s"DataPoint($currentlyOpenTabs, $openTabsGrouped, $openTabsUngrouped, $switchesWithinGroups, " +
      s"$switchesBetweenGroups, $switchesFromGroups, $switchesToGroups, $switchesOutsideGroups)"
  }
}
