package communitydetection

import persistence.Persistence
import tabswitches.SwitchGraphActor
import tabswitches.SwitchMapActor
import tabswitches.TabMeta
import tabswitches.TabSwitchMeta

trait CommunityDetectorParameters {

  /**
    * Ignore edges with a lower weight
    */
  def minWeight: Int = 2

  /**
    * The maximum number of groups to return
    */
  def maxGroups: Int = 10

  /**
    * Remove groups with less nodes
    */
  def minGroupSize: Int = 2

  /**
    * Remove groups with more nodes
    */
  def maxGroupSize: Int = 10

}

trait CommunityDetector[S, T <: CommunityDetectorParameters] {

  import tabswitches.TabSwitchActor.TabSwitchGraph

  def apply(
      graph: TabSwitchGraph,
      params: T
  ): List[(Set[TabMeta], CliqueStatistics)] = {

    if (graph == null) return List()

    val preparedGraph = prepareGraph(graph, params)

    val tabGroups = computeGroups(preparedGraph, params)

    processGroups(tabGroups, params)

  }

  def apply(
      graph: TabSwitchGraph,
      params: T,
      persistTo: String
  ): List[(Set[TabMeta], CliqueStatistics)] = {
    val tabGroups = apply(graph, params)

    persist(persistTo, tabGroups)

    tabGroups
  }

  def loadTestGraph: TabSwitchGraph = {

    var tabSwitchMap: Map[String, TabSwitchMeta] = null
    var tabSwitchGraph: TabSwitchGraph = null

    SwitchMapActor.restoreTabSwitchMap map {
      case Right(restoredMap) => {
        tabSwitchMap = restoredMap.toMap
        tabSwitchGraph = SwitchGraphActor.processSwitchMap(tabSwitchMap)
      }
      case _ =>
    }

    tabSwitchGraph
  }

  /**
    * Prepare the tab switch graph for analysis with the algorithm
    *
    * @param graph The raw tab switch graph
    * @return The pre-processed tab switch graph
    */
  def prepareGraph(graph: TabSwitchGraph, params: T): S

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
      .sortBy(_._2.quality)

    val topK = filteredGroups.take(params.maxGroups)

    println(topK)

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
      tabGroups: List[(Set[TabMeta], CliqueStatistics)]
  ) = {
    Persistence.persistString(
      fileName,
      tabGroups.map(_.toString()).mkString("\n")
    )
  }

}
