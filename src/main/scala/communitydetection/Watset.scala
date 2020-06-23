package communitydetection

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.nlpub.watset.graph.MarkovClustering
import tabswitches.TabMeta

object Watset extends App with LazyLogging with CommunityDetector {

  def prepareGraph(graph: TabSwitchGraph): TabSwitchGraph =
    graph

  def computeGroups(graph: TabSwitchGraph): List[Set[TabMeta]] = {

    val markovClusters = new MarkovClustering(graph, 2, 2)
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
