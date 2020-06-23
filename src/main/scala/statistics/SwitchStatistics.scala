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

  sealed trait Switch {
    val value: (Int, Int, Int, Int, Int)
  }

  case object SwitchWithinGroup extends Switch {
    val value = (1, 0, 0, 0, 0)
  }

  case object SwitchBetweenGroups extends Switch {
    val value = (0, 1, 0, 0, 0)
  }

  case object SwitchFromGroup extends Switch {
    val value = (0, 0, 1, 0, 0)
  }

  case object SwitchToGroup extends Switch {
    val value = (0, 0, 0, 1, 0)
  }

  case object SwitchOutside extends Switch {
    val value = (0, 0, 0, 0, 1)
  }
}
