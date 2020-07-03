package statistics

case class InteractionStatistics(
    acceptedGroups: Int,
    acceptedTabs: Int,
    discardedGroups: Int,
    discardedTabs: Int
)

object InteractionStatistics {
  def fromTuple(tuple: (Int, Int, Int, Int)) = {
    InteractionStatistics(tuple._1, tuple._2, tuple._3, tuple._4)
  }

  sealed trait Interaction {
    val value: (Int, Int, Int, Int)
  }

  case object AcceptedGroup extends Interaction {
    val value = (1, 0, 0, 0)
  }

  case object AcceptedTab extends Interaction {
    val value = (0, 1, 0, 0)
  }

  case object DiscardedGroup extends Interaction {
    val value = (0, 0, 1, 0)
  }

  case object DiscardedTab extends Interaction {
    val value = (0, 0, 0, 1)
  }
}
