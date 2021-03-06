package communitydetection

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.alg.clique.BronKerboschCliqueFinder
import tabswitches.TabMeta
import tabswitches.TabSwitchActor

case class BronKerboschParams(
    /**
      * The maximum number of groups to return
      */
    maxGroups: Int = 10,
    /**
      * Remove groups with less nodes
      */
    minGroupSize: Int = 3,
    /**
      * Remove groups with more nodes
      */
    maxGroupSize: Int = 10
) extends CommunityDetectorParameters

object BronKerbosch
// extends App
    extends LazyLogging
    with CommunityDetector[TabSwitchActor.TabSwitchGraph, BronKerboschParams] {

  // val testGraph = loadTestGraph

  // val tabGroups = apply(testGraph, BronKerboschParams())

  override def prepareGraph(
      graph: TabSwitchActor.TabSwitchGraph,
      pageRank: Map[TabMeta, Double],
      params: BronKerboschParams
  ): TabSwitchActor.TabSwitchGraph = graph

  override def computeGroups(
      graph: TabSwitchActor.TabSwitchGraph,
      params: BronKerboschParams
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    val cliques = new BronKerboschCliqueFinder(graph)

    cliques.asScala
      .map(_.asScala.toSet)
      .toList
      .map((_, CliqueStatistics()))
  }

}
