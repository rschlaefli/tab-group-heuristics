package communitydetection

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.nlpub.watset.graph._
import persistence.Persistence
import tabswitches.TabMeta
import tabswitches.TabSwitchActor

case class WatsetParams(expansion: Int = 2, powerCoefficient: Double = 2)
    extends Parameters

object Watset
    extends App
    with LazyLogging
    with CommunityDetector[TabSwitchActor.TabSwitchGraph, WatsetParams] {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  def prepareGraph(graph: TabSwitchGraph): TabSwitchGraph =
    graph

  def computeGroups(
      graph: TabSwitchGraph,
      params: WatsetParams
  ): List[Set[TabMeta]] = {

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
  }

  def processGroups(tabGroups: List[Set[TabMeta]]): List[Set[TabMeta]] = {
    Persistence.persistString(
      "clusters_watset.txt",
      tabGroups.map(_.toString()).mkString("\n")
    )

    tabGroups
  }

}
