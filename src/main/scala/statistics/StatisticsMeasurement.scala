package statistics

case class Age(
    lte1: Int = 0,
    lte2: Int = 0,
    lte5: Int = 0,
    lte10: Int = 0,
    lte30: Int = 0,
    lte60: Int = 0,
    gt60: Int = 0
) {

  def apply(num: Int): Age = num match {
    case num if num <= 1  => Age(lte1 = 1)
    case num if num <= 2  => Age(lte2 = 1)
    case num if num <= 5  => Age(lte5 = 1)
    case num if num <= 10 => Age(lte10 = 1)
    case num if num <= 30 => Age(lte30 = 1)
    case num if num <= 60 => Age(lte60 = 1)
    case _                => Age(gt60 = 1)
  }

  def +(other: Age) = Age(
    this.lte1 + other.lte1,
    this.lte2 + other.lte2,
    this.lte5 + other.lte5,
    this.lte10 + other.lte10,
    this.lte30 + other.lte30,
    this.lte60 + other.lte60,
    this.gt60 + other.gt60
  )

  def asCsv =
    Seq(
      this.lte1,
      this.lte2,
      this.lte5,
      this.lte10,
      this.lte30,
      this.lte60,
      this.gt60
    ).mkString(";")
}

case class Rating(
    is1: Int = 0,
    is2: Int = 0,
    is3: Int = 0,
    is4: Int = 0,
    is5: Int = 0
) {

  def apply(num: Int): Rating = num match {
    case num if num == 1 => Rating(is1 = 1)
    case num if num == 2 => Rating(is2 = 1)
    case num if num == 3 => Rating(is3 = 1)
    case num if num == 4 => Rating(is4 = 1)
    case num if num == 5 => Rating(is5 = 1)
    case _               => Rating()
  }

  def +(other: Rating) = Rating(
    this.is1 + other.is1,
    this.is2 + other.is2,
    this.is3 + other.is3,
    this.is4 + other.is4,
    this.is5 + other.is5
  )

  def asCsv =
    Seq(this.is1, this.is2, this.is3, this.is4, this.is5).mkString(";")
}

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

  def interactionCount: Int =
    this.numAcceptedGroups + this.numAcceptedTabs + this.numDiscardedGroups + this.numDiscardedTabs

}
