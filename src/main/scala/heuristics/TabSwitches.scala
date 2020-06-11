package heuristics

import com.typesafe.scalalogging.LazyLogging
import scalax.collection.io.json.descriptor.NodeDescriptor
import scalax.collection.io.json.descriptor.Descriptor
import scalax.collection.io.json.descriptor.predefined.WDi
import scalax.collection.io.json._
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot._
import scalax.collection.Graph, scalax.collection.GraphEdge._
import implicits._
import scala.collection.mutable.Map
import io.circe._, io.circe.parser._, io.circe.generic.semiauto._,
io.circe.syntax._
import scala.util.Try
import smile.math.MathEx._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.slf4j.MarkerFactory

import statistics.StatisticsEngine
import tabstate.{TabState, Tab}
import persistence.Persistable

object TabSwitches extends LazyLogging with Persistable {
  val logToCsv = MarkerFactory.getMarker("CSV")

  var tabSwitches = Map[String, Map[String, Int]]()
  var tabHashes = Map[String, String]()
  var tabGraph = Graph[Tab, WDiEdge]()

  var temp: Option[Tab] = None

  def processInitialTabs(initialTabs: List[Tab]) = {
    tabHashes ++= initialTabs.map(tab => (tab.hash, tab.baseUrl + tab.title))
    tabGraph ++= initialTabs
  }

  /**
    * Process a tab update for the tab switching heuristic
    * Updates the switching and hash maps and the tab switch graph
    *
    * @param previousTab
    * @param currentTab
    */
  def processTabSwitch(previousTab: Option[Tab], currentTab: Tab): Unit = {
    tabGraph += currentTab

    val tabHashContent =
      s"${currentTab.baseUrl} ${currentTab.normalizedTitle}"

    tabHashes.update(currentTab.hash, tabHashContent)

    if (previousTab.isDefined) {
      val prevTab = previousTab.get

      val switchFromNewTab = prevTab.title == "New Tab"
      val switchToNewTab = currentTab.title == "New Tab"

      logger.debug(
        s"Switch toNewTab=$switchToNewTab fromNewTab=$switchFromNewTab"
      )

      if (prevTab.hash != currentTab.hash) {
        if (switchToNewTab && !switchFromNewTab) {
          temp = Some(prevTab)
          return
        }

        if (temp.isDefined && switchFromNewTab && !switchToNewTab) {
          processSwitch(temp.get, currentTab)
          temp = None
        } else {
          processSwitch(prevTab, currentTab)
        }
      }
    }
  }

  def processSwitch(prevTab: Tab, currentTab: Tab): Unit = {
    Future {
      StatisticsEngine.tabSwitchQueue.synchronized {
        // add the tab switch to the statistics switch queue
        StatisticsEngine.tabSwitchQueue.enqueue((prevTab, currentTab))
      }
    }

    logger.info(
      logToCsv,
      s"${prevTab.id};${prevTab.hash};${prevTab.baseUrl};${prevTab.normalizedTitle};${currentTab.id};${currentTab.hash};${currentTab.baseUrl};${currentTab.normalizedTitle}"
    )

    tabSwitches.updateWith(prevTab.hash)((switchMap) => {
      val map = switchMap.getOrElse(Map((currentTab.hash, 0)))

      val previousCount = map.getOrElse(currentTab.hash, 0)

      tabGraph -= WDiEdge((prevTab, currentTab))(previousCount)
      tabGraph += WDiEdge((prevTab, currentTab))(previousCount + 1)

      map.update(currentTab.hash, previousCount + 1)

      Some(map)
    })
  }

  def cleanupGraph(graph: Graph[Tab, WDiEdge]): Graph[Tab, WDiEdge] = {
    // remove all edges that are recursive
    val graphWithoutSelfEdges = graph --
      graph.edges.filter(edge => edge.from.equals(edge.to))

    // remove the new tab page from the graph
    val graphWithoutNewTab =
      graphWithoutSelfEdges -- graphWithoutSelfEdges.nodes.filter(node =>
        node.value.title == "New Tab"
      )

    // extract all edge weights
    val edgeWeightsThreshold = median(
      graphWithoutNewTab.edges.map(edge => edge.weight).toArray
    ) / 2
    logger.debug(s"> Computed threshold for edge weights $edgeWeightsThreshold")

    // remove all nodes that have a very low incoming weight
    // i.e., remove nodes that have been switched to few times
    val irrelevantNodes = graphWithoutNewTab.nodes.filter(node =>
      node.incoming.map(edge => edge.weight).sum < edgeWeightsThreshold
    )
    val graphWithoutIrrelevantNodes = graphWithoutNewTab -- irrelevantNodes

    // remove all edges that have been traversed only few times
    // i.e., get rid of tab switches that have only occured few times
    val irrelevantEdges =
      graphWithoutIrrelevantNodes.edges.filter(edge =>
        edge.weight < edgeWeightsThreshold
      )
    val graphWithoutIrrelevantEdges =
      graphWithoutIrrelevantNodes -- irrelevantEdges

    logger.debug(
      s"> Performed graph cleanup: removed ${irrelevantEdges.size} edges and ${irrelevantNodes.size} nodes - " +
        s"left with ${graphWithoutIrrelevantEdges.edges.size}/${graphWithoutIrrelevantEdges.nodes.size} of ${graph.edges.size}/${graph.nodes.size} edges/nodes"
    )

    graphWithoutIrrelevantEdges
  }

  val jsonTabDescriptor = new NodeDescriptor[Tab](typeId = "Tabs") {
    def id(node: Any) = node match {
      case tab: Tab => tab.hash
    }
  }

  val jsonGraphDescriptor = new Descriptor[Tab](
    defaultNodeDescriptor = jsonTabDescriptor,
    defaultEdgeDescriptor = WDi.descriptor[Tab]()
  )

  def toJsonString(graph: Graph[Tab, WDiEdge]): String = {
    graph.toJson(jsonGraphDescriptor)
  }

  def fromJsonString(jsonString: String) = {
    Graph.fromJson[Tab, WDiEdge](jsonString, jsonGraphDescriptor)
  }

  val dotRoot = DotRootGraph(
    directed = true,
    id = Some("Tabs")
    // attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr("shape", "record")))),
    // attrList = List(DotAttr("attr_1", """"one""""), DotAttr("attr_2", "<two>"))
  )

  def edgeTransformer(
      innerEdge: Graph[Tab, WDiEdge]#EdgeT
  ): Option[(DotGraph, DotEdgeStmt)] = innerEdge.edge match {
    case WDiEdge(source, target, weight) =>
      weight match {
        case weight: Double =>
          Some(
            (
              dotRoot,
              DotEdgeStmt(
                source.toString,
                target.toString,
                List(
                  DotAttr("weight", weight.toInt),
                  DotAttr("label", weight.toInt.toString)
                )
              )
            )
          )
      }
  }

  /**
    * Convert a tab switch graph into a DOT string that can be read with graphviz
    * ref: https://graphviz.gitlab.io/_pages/pdf/dotguide.pdf
    *
    * @param graph A directed, weighted tab switch graph
    * @return A string in the DOT format
    */
  def toDotString(graph: Graph[Tab, WDiEdge]): String = {
    graph.toDot(dotRoot, edgeTransformer)
  }

  def persist: Try[Unit] = Try {
    Persistable
      .persistJson("tab_hashes.json", tabHashes.asJson)

    Persistable
      .persistJson(
        "tab_switches.json",
        tabSwitches.asJson
      )

    Persistable
      .persistString(
        "tab_switch_graph.json",
        toJsonString(tabGraph)
      )

    Persistable
      .persistString(
        "tab_switch_graph.dot",
        toDotString(tabGraph)
      )
    Persistable.persistString(
      "tab_switch_graph_clean.dot",
      toDotString(cleanupGraph(tabGraph))
    )
  }

  def restore: Try[Unit] = Try {
    Persistable
      .restoreJson("tab_hashes.json")
      .map(decode[Map[String, String]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabHashes = restoredMap
      }

    Persistable
      .restoreJson("tab_switches.json")
      .map(decode[Map[String, Map[String, Int]]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabSwitches = restoredMap
      }

    Persistable
      .restoreJson("tab_switch_graph.json")
      .map(TabSwitches.fromJsonString)
      .foreach(tabGraph = _)
  }
}
