package communitydetection

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.alg.scoring.PageRank
import persistence.Persistence
import tabswitches.TabMeta

trait CommunityDetector[S, T <: CommunityDetectorParameters]
    extends LazyLogging {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  def apply(
      graph: TabSwitchGraph,
      params: T
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    if (graph == null) return List()

    val pageRank = new PageRank(graph)
      .getScores()
      .asScala
      .map(entry => (entry._1, entry._2.toDouble))
      .toMap

    val preparedGraph = prepareGraph(graph, pageRank, params)

    val tabGroups = computeGroups(preparedGraph, params)

    processGroups(tabGroups, params)

  }

  def apply(
      graph: TabSwitchGraph,
      params: T,
      persistTo: String
  ): List[(Set[TabMeta], CliqueStatistics)] = {
    val tabGroups = apply(graph, params)

    persist(persistTo, tabGroups, params)

    tabGroups
  }

  // def loadTestGraph: TabSwitchGraph = {

  //   var tabSwitchMap: Map[String, TabSwitchMeta] = null
  //   var tabSwitchGraph: TabSwitchGraph = null

  //   SwitchMapActor.restoreTabSwitchMap map {
  //     case Right(restoredMap) => {
  //       tabSwitchMap = restoredMap.toMap
  //       tabSwitchGraph = SwitchGraphActor.processSwitchMap(tabSwitchMap)
  //     }
  //     case _ =>
  //   }

  //   tabSwitchGraph
  // }

  /**
    * Prepare the tab switch graph for analysis with the algorithm
    *
    * @param graph The raw tab switch graph
    * @return The pre-processed tab switch graph
    */
  def prepareGraph(
      graph: TabSwitchGraph,
      pageRank: Map[TabMeta, Double],
      params: T
  ): S

  /**
    * Apply the community detection algorithm to the tab switch graph
    *
    * @param graph The pre-processed tab switch graph
    * @return The generated list of tab groups
    */
  def computeGroups(
      graph: S,
      params: T
  ): List[(Set[TabMeta], CliqueStatistics)]

  /**
    * Perform postprocessing on the generated tab groups
    *
    * @param tabGroups The generated list of tab groups
    * @return The post-processed list of tab groups
    */
  def processGroups(
      tabGroups: List[(Set[TabMeta], CliqueStatistics)],
      params: T
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    val filteredGroups = tabGroups
      .filter(group =>
        params.maxGroupSize >= group._1.size
          && group._1.size >= params.minGroupSize
      )
      .sortBy(_._2.score)

    val topK = filteredGroups.reverse.take(params.maxGroups)

    topK

  }

  /**
    * Persist the list of tab groups to a text file
    *
    * @param fileName The name of the target file
    * @param tabGroups The list of tab groups
    * @return
    */
  def persist(
      fileName: String,
      tabGroups: List[(Set[TabMeta], CliqueStatistics)],
      params: T
  ) = {
    Persistence.persistString(
      fileName,
      tabGroups.map(_.toString()).appended(params.toString()).mkString("\n")
    )
  }

}
