package statistics

case class SwitchStatistics(
    switchesWithinGroups: Int,
    switchesBetweenGroups: Int,
    switchesFromGroups: Int,
    switchesToGroups: Int,
    switchesOutsideGroups: Int
)

object SwitchStatistics {
  def fromTuple(tuple: (Int, Int, Int, Int, Int)) = {
    SwitchStatistics(tuple._1, tuple._2, tuple._3, tuple._4, tuple._5)
  }
}
