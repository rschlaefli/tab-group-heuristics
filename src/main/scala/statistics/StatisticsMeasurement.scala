package statistics

case class StatisticsMeasurement(
    currentlyOpenTabs: Int = 0,
    openTabsUngrouped: Int = 0,
    openTabsGrouped: Int = 0,
    averageTabAge: Double = 0d,
    averageTabStaleDuration: Double = 0d,
    switchesWithinGroups: Int = 0,
    switchesBetweenGroups: Int = 0,
    switchesFromGroups: Int = 0,
    switchesToGroups: Int = 0,
    switchesOutsideGroups: Int = 0,
    acceptedGroups: Int = 0,
    acceptedTabs: Int = 0,
    discardedGroups: Int = 0,
    discardedTabs: Int = 0
) {

  def asSeq = Seq(
    this.currentlyOpenTabs,
    this.openTabsUngrouped,
    this.openTabsGrouped,
    this.averageTabAge,
    this.averageTabStaleDuration,
    this.switchesWithinGroups,
    this.switchesBetweenGroups,
    this.switchesFromGroups,
    this.switchesToGroups,
    this.switchesOutsideGroups,
    this.acceptedGroups,
    this.acceptedTabs,
    this.discardedGroups,
    this.discardedTabs
  )

  def asTuple = (
    this.currentlyOpenTabs,
    this.openTabsUngrouped,
    this.openTabsGrouped,
    this.averageTabAge,
    this.averageTabStaleDuration,
    this.switchesWithinGroups,
    this.switchesBetweenGroups,
    this.switchesFromGroups,
    this.switchesToGroups,
    this.switchesOutsideGroups,
    this.acceptedGroups,
    this.acceptedTabs,
    this.discardedGroups,
    this.discardedTabs
  )

  def +(other: StatisticsMeasurement) = {
    StatisticsMeasurement(
      currentlyOpenTabs =
        this.currentlyOpenTabs + other.currentlyOpenTabs,
      openTabsUngrouped =
        this.openTabsUngrouped + other.openTabsUngrouped,
      openTabsGrouped =
        this.openTabsGrouped + other.openTabsGrouped,
      averageTabAge =
        this.averageTabAge + other.averageTabAge,
      averageTabStaleDuration =
        this.averageTabStaleDuration + other.averageTabStaleDuration,
      switchesWithinGroups =
        this.switchesWithinGroups + other.switchesWithinGroups,
      switchesBetweenGroups =
        this.switchesBetweenGroups + other.switchesBetweenGroups,
      switchesFromGroups =
        this.switchesFromGroups + other.switchesFromGroups,
      switchesToGroups =
        this.switchesToGroups + other.switchesToGroups,
      switchesOutsideGroups =
        this.switchesOutsideGroups + other.switchesOutsideGroups,
      acceptedGroups =
        this.acceptedGroups + other.acceptedGroups,
      acceptedTabs =
        this.acceptedTabs + other.acceptedTabs,
      discardedGroups =
        this.discardedGroups + other.discardedGroups,
      discardedTabs =
        this.discardedTabs + other.discardedTabs
    )
  }

  def interactionCount: Int =
    this.acceptedGroups + this.acceptedTabs + this.discardedGroups + this.discardedTabs

}
