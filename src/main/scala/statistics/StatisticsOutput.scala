package statistics

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
