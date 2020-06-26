package communitydetection

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.nlpub.watset.graph._
import tabswitches.TabMeta
import tabswitches.TabSwitchActor

case class WatsetParams(expansion: Int = 2, powerCoefficient: Double = 2)
    extends CommunityDetectorParameters

object Watset
// extends App
    extends LazyLogging
    with CommunityDetector[TabSwitchActor.TabSwitchGraph, WatsetParams] {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  def prepareGraph(
      graph: TabSwitchGraph,
      pageRank: Map[TabMeta, Double],
      params: WatsetParams
  ): TabSwitchGraph = graph

  def computeGroups(
      graph: TabSwitchGraph,
      params: WatsetParams
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    val isGraphTooSmall = graph
      .vertexSet()
      .size() < 2
    val isGraphUnconnected = graph.edgeSet().size() == 0

    if (isGraphTooSmall || isGraphUnconnected) return List()

    val markovClusters =
      new MarkovClustering(graph, params.expansion, params.powerCoefficient)
    markovClusters.fit()

    markovClusters
      .getClusters()
      .asScala
      .map(_.asScala.toSet)
      .toList
      .map((_, CliqueStatistics()))
  }

}
