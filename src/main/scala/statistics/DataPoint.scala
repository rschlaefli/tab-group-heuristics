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

  var acceptedGroups: Int = 0
  var acceptedTabs: Int = 0
  var discardedGroups: Int = 0
  var discardedTabs: Int = 0

  def updateSwitchStatistics(switchStatistics: SwitchStatistics): Unit = {
    switchesWithinGroups = switchStatistics.switchesWithinGroups
    switchesBetweenGroups = switchStatistics.switchesBetweenGroups
    switchesFromGroups = switchStatistics.switchesFromGroups
    switchesToGroups = switchStatistics.switchesToGroups
    switchesOutsideGroups = switchStatistics.switchesOutsideGroups
  }

  def updateSuggestionInteractionStatistics(
      interactionStatistics: InteractionStatistics
  ): Unit = {
    acceptedGroups = interactionStatistics.acceptedGroups
    acceptedTabs = interactionStatistics.acceptedTabs
    discardedGroups = interactionStatistics.discardedGroups
    discardedTabs = interactionStatistics.discardedTabs
  }

  override def toString(): String = {
    s"DataPoint($currentlyOpenTabs, $openTabsGrouped, $openTabsUngrouped, $switchesWithinGroups, " +
      s"$switchesBetweenGroups, $switchesFromGroups, $switchesToGroups, $switchesOutsideGroups)"
  }
}
