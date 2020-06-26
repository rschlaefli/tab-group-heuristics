package communitydetection

case class CliqueStatistics(quality: Double = 0)

object CliqueStatistics {
  def apply(stats: Set[NodeStatistics]): CliqueStatistics = {
    val (aggInWeight, aggOutWeight) = stats
      .map(stat => (stat.inWeight, stat.outWeight))
      .foldLeft((0d, 0d)) {
        case (acc, tuple) =>
          (acc._1 + tuple._1, acc._2 + tuple._2)
      }

    CliqueStatistics(aggInWeight / aggOutWeight)
  }
}
