package statistics

case class StatisticsMeasurement(
    numOpenTabs: Int = 0,
    numOpenTabsGrouped: Int = 0,
    numOpenTabsUngrouped: Int = 0,
    numSwitchesBetweenGroups: Int = 0,
    numSwitchesWithinGroups: Int = 0,
    numSwitchesFromGroups: Int = 0,
    numSwitchesToGroups: Int = 0,
    numSwitchesUngrouped: Int = 0,
    numCuratedGroups: Int = 0,
    numCuratedGroupsOpened: Int = 0,
    numCuratedGroupsClosed: Int = 0,
    numFocusModeUsed: Int = 0,
    numAcceptedGroups: Int = 0,
    numAcceptedTabs: Int = 0,
    numDiscardedGroups: Int = 0,
    numDiscardedTabs: Int = 0,
    numDiscardedWrong: Int = 0,
    numDiscardedOther: Int = 0,
    // as there can be multiple discards and switches within a 20sec window
    // we need to have a sequence of values for the next two properties
    binDiscardedRatings: Seq[Rating] = Seq(),
    binSwitchTime: Seq[Age] = Seq(),
    // contrary to the above, tab age and staleness are "static"
    // as they are computed from the 20sec snapshot of tabs
    binTabAge: Age = Age(),
    binTabStaleness: Age = Age()
) {

  def asSeq: Seq[Any] = Seq(
    this.numOpenTabs,
    this.numOpenTabsGrouped,
    this.numOpenTabsUngrouped,
    this.numSwitchesBetweenGroups,
    this.numSwitchesWithinGroups,
    this.numSwitchesFromGroups,
    this.numSwitchesToGroups,
    this.numSwitchesUngrouped,
    this.numCuratedGroups,
    this.numCuratedGroupsOpened,
    this.numCuratedGroupsClosed,
    this.numFocusModeUsed,
    this.numAcceptedGroups,
    this.numAcceptedTabs,
    this.numDiscardedGroups,
    this.numDiscardedTabs,
    this.numDiscardedWrong,
    this.numDiscardedOther,
    this.binDiscardedRatings.fold(Rating())(_ + _),
    this.binSwitchTime.fold(Age())(_ + _),
    this.binTabAge,
    this.binTabStaleness
  )

  def +(other: StatisticsMeasurement) = {
    StatisticsMeasurement(
      this.numOpenTabs + other.numOpenTabs,
      this.numOpenTabsGrouped + other.numOpenTabsGrouped,
      this.numOpenTabsUngrouped + other.numOpenTabsUngrouped,
      this.numSwitchesBetweenGroups + other.numSwitchesBetweenGroups,
      this.numSwitchesWithinGroups + other.numSwitchesWithinGroups,
      this.numSwitchesFromGroups + other.numSwitchesFromGroups,
      this.numSwitchesToGroups + other.numSwitchesToGroups,
      this.numSwitchesUngrouped + other.numSwitchesUngrouped,
      this.numCuratedGroups + other.numCuratedGroups,
      this.numCuratedGroupsOpened + other.numCuratedGroupsOpened,
      this.numCuratedGroupsClosed + other.numCuratedGroupsClosed,
      this.numFocusModeUsed + other.numFocusModeUsed,
      this.numAcceptedGroups + other.numAcceptedGroups,
      this.numAcceptedTabs + other.numAcceptedTabs,
      this.numDiscardedGroups + other.numDiscardedGroups,
      this.numDiscardedTabs + other.numDiscardedTabs,
      this.numDiscardedWrong + other.numDiscardedWrong,
      this.numDiscardedOther + other.numDiscardedOther,
      this.binDiscardedRatings ++ other.binDiscardedRatings,
      this.binSwitchTime ++ other.binSwitchTime,
      this.binTabAge + other.binTabAge,
      this.binTabStaleness + other.binTabStaleness
    )
  }

  def asCsv: String =
    Seq(
      numOpenTabs,
      numOpenTabsGrouped,
      numOpenTabsUngrouped,
      numSwitchesBetweenGroups,
      numSwitchesWithinGroups,
      numSwitchesFromGroups,
      numSwitchesToGroups,
      numSwitchesUngrouped,
      numCuratedGroups,
      numCuratedGroupsOpened,
      numCuratedGroupsClosed,
      numFocusModeUsed,
      numAcceptedGroups,
      numAcceptedTabs,
      numDiscardedGroups,
      numDiscardedTabs,
      numDiscardedWrong,
      numDiscardedOther,
      binDiscardedRatings.fold(Rating())(_ + _).asCsv,
      binSwitchTime.fold(Age())(_ + _).asCsv,
      binTabAge.asCsv,
      binTabStaleness.asCsv
    ).mkString(";")

  def interactionCount: Int =
    this.numAcceptedGroups + this.numAcceptedTabs + this.numDiscardedGroups + this.numDiscardedTabs

}
