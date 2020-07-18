package statistics

import smile.math.MathEx._

case class StatisticsOutput(
    avgNumOpenTabs: Double,
    avgNumOpenTabsGrouped: Double,
    avgNumOpenTabsUngrouped: Double,
    numSwitchesBetweenGroups: Int = 0,
    numSwitchesWithinGroups: Int = 0,
    numSwitchesFromGroups: Int = 0,
    numSwitchesToGroups: Int = 0,
    numSwitchesUngrouped: Int = 0,
    avgNumCuratedGroups: Double = 0,
    numCuratedGroupsOpened: Int = 0,
    numCuratedGroupsClosed: Int = 0,
    numFocusModeUsed: Int = 0,
    numAcceptedGroups: Int = 0,
    numAcceptedTabs: Int = 0,
    numDiscardedGroups: Int = 0,
    numDiscardedTabs: Int = 0,
    numDiscardedWrong: Int = 0,
    numDiscardedOther: Int = 0,
    binDiscardedRatings: Rating,
    binDwitchTime: Age,
    binTabAge: Age,
    binTabStaleness: Age
) {

  def asCsv: String =
    Seq(
      avgNumOpenTabs,
      avgNumOpenTabsGrouped,
      avgNumOpenTabsUngrouped,
      numSwitchesBetweenGroups,
      numSwitchesWithinGroups,
      numSwitchesFromGroups,
      numSwitchesToGroups,
      numSwitchesUngrouped,
      avgNumCuratedGroups,
      numCuratedGroupsOpened,
      numCuratedGroupsClosed,
      numFocusModeUsed,
      numAcceptedGroups,
      numAcceptedTabs,
      numDiscardedGroups,
      numDiscardedTabs,
      numDiscardedWrong,
      numDiscardedOther,
      binDiscardedRatings.asCsv,
      binDwitchTime.asCsv,
      binTabAge.asCsv,
      binTabStaleness.asCsv
    ).mkString(";")

}

object StatisticsOutput {
  def apply(measurements: List[StatisticsMeasurement]): StatisticsOutput = {
    val transposed = measurements.map(_.asSeq).transpose

    StatisticsOutput(
      avgNumOpenTabs =
        round(mean(transposed(0).map(_.asInstanceOf[Int]).toArray), 2),
      avgNumOpenTabsGrouped =
        round(mean(transposed(1).map(_.asInstanceOf[Int]).toArray), 2),
      avgNumOpenTabsUngrouped =
        round(mean(transposed(2).map(_.asInstanceOf[Int]).toArray), 2),
      numSwitchesBetweenGroups = transposed(3).map(_.asInstanceOf[Int]).sum,
      numSwitchesWithinGroups = transposed(4).map(_.asInstanceOf[Int]).sum,
      numSwitchesFromGroups = transposed(5).map(_.asInstanceOf[Int]).sum,
      numSwitchesToGroups = transposed(6).map(_.asInstanceOf[Int]).sum,
      numSwitchesUngrouped = transposed(7).map(_.asInstanceOf[Int]).sum,
      avgNumCuratedGroups =
        round(mean(transposed(8).map(_.asInstanceOf[Int]).toArray), 2),
      numCuratedGroupsOpened = transposed(9).map(_.asInstanceOf[Int]).sum,
      numCuratedGroupsClosed = transposed(10).map(_.asInstanceOf[Int]).sum,
      numFocusModeUsed = transposed(11).map(_.asInstanceOf[Int]).sum,
      numAcceptedGroups = transposed(12).map(_.asInstanceOf[Int]).sum,
      numAcceptedTabs = transposed(13).map(_.asInstanceOf[Int]).sum,
      numDiscardedGroups = transposed(14).map(_.asInstanceOf[Int]).sum,
      numDiscardedTabs = transposed(15).map(_.asInstanceOf[Int]).sum,
      numDiscardedWrong = transposed(16).map(_.asInstanceOf[Int]).sum,
      numDiscardedOther = transposed(17).map(_.asInstanceOf[Int]).sum,
      binDiscardedRatings =
        transposed(18).map(_.asInstanceOf[Rating]).fold(Rating())(_ + _),
      binDwitchTime =
        transposed(19).map(_.asInstanceOf[Age]).fold(Age())(_ + _),
      binTabAge = transposed(20).map(_.asInstanceOf[Age]).fold(Age())(_ + _),
      binTabStaleness =
        transposed(21).map(_.asInstanceOf[Age]).fold(Age())(_ + _)
    )

  }
}
