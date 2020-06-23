package communitydetection

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.nlpub.watset.graph._
import tabswitches.TabMeta

case class WatsetParams(expansion: Int, powerCoefficient: Double)
    extends Parameters

object Watset
    extends App
    with LazyLogging
    with CommunityDetector[WatsetParams] {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  def prepareGraph(graph: TabSwitchGraph): TabSwitchGraph =
    graph

  def computeGroups(
      graph: TabSwitchGraph,
      params: WatsetParams
  ): List[Set[TabMeta]] = {
    val markovClusters =
      new MarkovClustering(graph, params.expansion, params.powerCoefficient)
    markovClusters.fit()

    logger.debug(
      s"Clusters (markov): ${markovClusters.getClusters().toString()}"
    )

    markovClusters
      .getClusters()
      .asScala
      .map(_.asScala.toSet)
      .toList
  }

  def processGroups(tabGroups: List[Set[TabMeta]]): List[Set[TabMeta]] =
    tabGroups

}
