package util

import scala.collection.mutable

import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import tabswitches.TabMeta

object Utils extends LazyLogging {
  def extractDecoderResult[T](
      decoderResult: Decoder.Result[T]
  ): Option[T] = decoderResult match {
    case Left(decodeError) => {
      logger.error(decodeError.message + decoderResult.toString())
      None
    }
    case Right(value) => Some(value)
  }

  def processClusters(
      clusters: List[Set[TabMeta]]
  ): (Map[Int, Int], List[Set[TabMeta]]) = {
    // preapre an index for which tab is stored in which cluster
    val clusterIndex = mutable.Map[Int, Int]()

    // prepare a return container for the clusters
    val clusterList = clusters.zipWithIndex.flatMap {
      case (clusterMembers, index) if clusterMembers.size > 1 => {
        clusterMembers.foreach(tab => {
          clusterIndex(tab.hashCode()) = index
        })

        logger.debug(s"Cluster $index contains ${clusterMembers.toString()}")

        List(clusterMembers)
      }
      case _ => List()
    }

    logger.debug(
      s"Computed overall index $clusterIndex for ${clusterList.length} clusters"
    )

    (Map.from(clusterIndex), clusterList)
  }
}
