package statistics

case class StatisticsOutput(
    numCurrentTabs: Double,
    openTabsGrouped: Double,
    openTabsUngrouped: Double,
    tabSwitchWithinGroups: Int,
    tabSwitchBetweenGroups: Int,
    tabSwitchFromGroup: Int,
    tabSwitchToGroup: Int,
    tabSwitchUngrouped: Int
) {
  def asCsv: String = {
    s"$numCurrentTabs;$openTabsGrouped;$openTabsUngrouped;$tabSwitchWithinGroups;$tabSwitchBetweenGroups;$tabSwitchFromGroup;$tabSwitchToGroup;$tabSwitchUngrouped"
  }
}
