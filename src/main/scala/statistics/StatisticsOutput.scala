package statistics

case class StatisticsOutput(
    numCurrentTabs: Float,
    openTabsGrouped: Float,
    openTabsUngrouped: Float,
    tabSwitchWithinGroups: Float,
    tabSwitchBetweenGroups: Float,
    tabSwitchFromGroup: Float,
    tabSwitchToGroup: Float,
    tabSwitchUngrouped: Float
) {
  def asCsv: String = {
    s"$numCurrentTabs;$openTabsGrouped;$openTabsUngrouped;$tabSwitchWithinGroups;$tabSwitchBetweenGroups;$tabSwitchFromGroup;$tabSwitchToGroup;$tabSwitchUngrouped"
  }
}
