package statistics

case class StatisticsMeasurement(
    numOpenWindows: Int = 0,
    numOpenTabs: Int = 0,
    numOpenTabsGrouped: Int = 0,
    numOpenTabsUngrouped: Int = 0,
    numSwitchesBetweenGroups: Int = 0,
    numSwitchesWithinGroups: Int = 0,
    numSwitchesFromGroups: Int = 0,
    numSwitchesToGroups: Int = 0,
    numSwitchesUngrouped: Int = 0,
    numSuggestedGroups: Int = 0,
    numCuratedGroups: Int = 0,
    numCuratedGroupsOpened: Int = 0,
    numCuratedGroupsClosed: Int = 0,
    numFocusModeUsed: Int = 0,
    numAcceptedGroups: Int = 0,
    numAcceptedTabs: Int = 0,
    numDiscardedGroups: Int = 0,
    numDiscardedTabs: Int = 0,
    numDiscardedWrong: Int = 0,
    numDiscardedNotUseful: Int = 0,
    numDiscardedOther: Int = 0,
    // as there can be multiple discards and switches within a 20sec window
    // we need to have a sequence of values for the next two properties
    binDiscardedRatings: Seq[Rating] = Seq(),
    binSwitchTime: Seq[Age] = Seq(),
    // contrary to the above, tab age and staleness are "static"
    // as they are computed from the 20sec snapshot of tabs
    binTabAge: Age = Age(),
    binTabStaleness: Age = Age(),
    // distribution of tabs in windows
    avgTabsPerWindow: Double = 0,
    stdTabsPerWindow: Double = 0
) {

  def +(other: StatisticsMeasurement) = {
    StatisticsMeasurement(
      numOpenWindows = this.numOpenWindows + other.numOpenWindows,
      numOpenTabs = this.numOpenTabs + other.numOpenTabs,
      numOpenTabsGrouped = this.numOpenTabsGrouped + other.numOpenTabsGrouped,
      numOpenTabsUngrouped =
        this.numOpenTabsUngrouped + other.numOpenTabsUngrouped,
      numSwitchesBetweenGroups =
        this.numSwitchesBetweenGroups + other.numSwitchesBetweenGroups,
      numSwitchesWithinGroups =
        this.numSwitchesWithinGroups + other.numSwitchesWithinGroups,
      numSwitchesFromGroups =
        this.numSwitchesFromGroups + other.numSwitchesFromGroups,
      numSwitchesToGroups =
        this.numSwitchesToGroups + other.numSwitchesToGroups,
      numSwitchesUngrouped =
        this.numSwitchesUngrouped + other.numSwitchesUngrouped,
      numCuratedGroups = this.numCuratedGroups + other.numCuratedGroups,
      numSuggestedGroups = this.numSuggestedGroups + other.numSuggestedGroups,
      numCuratedGroupsOpened =
        this.numCuratedGroupsOpened + other.numCuratedGroupsOpened,
      numCuratedGroupsClosed =
        this.numCuratedGroupsClosed + other.numCuratedGroupsClosed,
      numFocusModeUsed = this.numFocusModeUsed + other.numFocusModeUsed,
      numAcceptedGroups = this.numAcceptedGroups + other.numAcceptedGroups,
      numAcceptedTabs = this.numAcceptedTabs + other.numAcceptedTabs,
      numDiscardedGroups = this.numDiscardedGroups + other.numDiscardedGroups,
      numDiscardedTabs = this.numDiscardedTabs + other.numDiscardedTabs,
      numDiscardedWrong = this.numDiscardedWrong + other.numDiscardedWrong,
      numDiscardedNotUseful =
        this.numDiscardedNotUseful + other.numDiscardedNotUseful,
      numDiscardedOther = this.numDiscardedOther + other.numDiscardedOther,
      binDiscardedRatings =
        this.binDiscardedRatings ++ other.binDiscardedRatings,
      binSwitchTime = this.binSwitchTime ++ other.binSwitchTime,
      binTabAge = this.binTabAge + other.binTabAge,
      binTabStaleness = this.binTabStaleness + other.binTabStaleness,
      avgTabsPerWindow = this.avgTabsPerWindow + other.avgTabsPerWindow,
      stdTabsPerWindow = this.stdTabsPerWindow + other.stdTabsPerWindow
    )
  }

  def asCsv: String =
    Seq(
      numOpenWindows,
      numOpenTabs,
      numOpenTabsGrouped,
      numOpenTabsUngrouped,
      numSwitchesBetweenGroups,
      numSwitchesWithinGroups,
      numSwitchesFromGroups,
      numSwitchesToGroups,
      numSwitchesUngrouped,
      numSuggestedGroups,
      numCuratedGroups,
      numCuratedGroupsOpened,
      numCuratedGroupsClosed,
      numFocusModeUsed,
      numAcceptedGroups,
      numAcceptedTabs,
      numDiscardedGroups,
      numDiscardedTabs,
      numDiscardedWrong,
      numDiscardedNotUseful,
      numDiscardedOther,
      binDiscardedRatings.fold(Rating())(_ + _).asCsv,
      binSwitchTime.fold(Age())(_ + _).asCsv,
      binTabAge.asCsv,
      binTabStaleness.asCsv,
      avgTabsPerWindow,
      stdTabsPerWindow
    ).mkString(";")

  def interactionCount: Int =
    this.numAcceptedGroups + this.numAcceptedTabs + this.numDiscardedGroups + this.numDiscardedTabs

}
