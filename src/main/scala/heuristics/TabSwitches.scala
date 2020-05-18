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

import tabstate.{Tabs, Tab, TabState}
import persistence.Persistable

object TabSwitches extends LazyLogging with Persistable {

  var tabSwitches = Map[String, Map[String, Int]]()
  var tabHashes = Map[String, String]()
  var tabGraph = Graph[Tab, WDiEdge]()

  // var tabBaseSwitches = Map[String, Map[String, Int]]()
  // var tabBaseHashes = Map[String, String]()
  // var tabBaseGraph = Graph[Tab, WDiEdge]()

  // var tabOriginSwitches = Map[String, Map[String, Int]]()
  // var tabOriginHashes = Map[String, String]()
  // var tabOriginGraph = Graph[Tab, WDiEdge]()

  def cleanupGraph(graph: Graph[Tab, WDiEdge]): Graph[Tab, WDiEdge] = {

    // remove all edges that are recursive
    val graphWithoutSelfEdges = graph --
      graph.edges.filter(edge => edge.from.equals(edge.to))

    // remove the new tab "root"
    // val graphWithoutNewTab =
    //   graphWithoutSelfEdges -- graphWithoutSelfEdges.nodes.filter(node =>
    //     node.hashCode() == -836262972 || node.hashCode() == -35532496
    //   )

    // // extract all edge weights
    // TODO: extract all edges except values in the top-5% or similar
    // val edgeWeights = q3(graph.edges.map(edge => edge.weight))

    // remove all edges that have been traversed only few times
    // i.e., get rid of tab switches that have only occured few times
    val graphWithoutIrrelevantEdges =
      graphWithoutSelfEdges -- graphWithoutSelfEdges.edges.filter(edge =>
        edge.weight < 5
      // edge.weight < 1
      )

    // remove all nodes that have a very low incoming weight
    // i.e., remove nodes that have been switched to few times
    graphWithoutIrrelevantEdges -- graphWithoutIrrelevantEdges.nodes.filter(
      node => node.incoming.map(edge => edge.weight).sum < 10
      // node => node.incoming.map(edge => edge.weight).sum < 1
    )

  }

  def processInitialTabs(initialTabs: List[Tab]) = {
    tabHashes ++= initialTabs.map(tab => (tab.hash, tab.baseUrl + tab.title))
    // tabBaseHashes ++= initialTabs.map(tab => (tab.baseHash, tab.baseUrl))
    // tabOriginHashes ++= initialTabs.map(tab => (tab.originHash, tab.origin))

    tabGraph ++= initialTabs
    // tabBaseGraph ++= initialTabs
    // tabOriginGraph ++= initialTabs
  }

  /**
    * Process a tab update for the tab switching heuristic
    * Updates the switching and hash maps and the tab switch graph
    *
    * @param previousTab
    * @param currentTab
    */
  def processTabSwitch(previousTab: Option[Tab], currentTab: Tab) {
    tabGraph += currentTab
    // tabBaseGraph += currentTab
    // tabOriginGraph += currentTab

    tabHashes.update(currentTab.hash, currentTab.baseUrl + currentTab.title)
    // tabBaseHashes.update(currentTab.baseHash, currentTab.baseUrl)
    // tabOriginHashes.update(currentTab.originHash, currentTab.origin)

    // check if the location of the tab changes in this update
    // if yes, we need to also account for the tab switch
    // TODO: does this make sense or is that update only happening if we leave a tab completely?
    if (previousTab.isDefined) {
      val prevTab = previousTab.get

      if (prevTab.hash != currentTab.hash) {
        logger.info(
          s"Processing fake tab switch for tab ${currentTab.id}"
        )

        tabSwitches.updateWith(prevTab.hash)((switchMap) => {
          val map = switchMap.getOrElse(Map((currentTab.hash, 0)))

          val previousCount = map.getOrElse(currentTab.hash, 0)

          tabGraph -= WDiEdge((prevTab, currentTab))(previousCount)
          tabGraph += WDiEdge((prevTab, currentTab))(
            previousCount + 1
          )

          map.update(currentTab.hash, previousCount + 1)

          Some(map)
        })
      }

      // if (prevTab.originHash != currentTab.originHash) {
      //   logger.info(
      //     s"Processing fake tab switch (origin) for tab ${currentTab.id}"
      //   )

      //   tabOriginSwitches.updateWith(prevTab.originHash)((switchMap) => {
      //     val map = switchMap
      //       .getOrElse(Map((currentTab.originHash, 0)))

      //     val previousCount = map.getOrElse(currentTab.originHash, 0)

      //     tabOriginGraph -= WDiEdge((prevTab, currentTab))(previousCount)
      //     tabOriginGraph += WDiEdge((prevTab, currentTab))(
      //       previousCount + 1
      //     )

      //     map.update(currentTab.originHash, previousCount + 1)

      //     Some(map)
      //   })
      // }
    }
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
    // Persistable
    //   .persistJson(
    //     "tab_hashes_origin.json",
    //     tabOriginHashes.asJson
    //   )

    Persistable
      .persistJson(
        "tab_switches.json",
        tabSwitches.asJson
      )
    // Persistable
    //   .persistJson(
    //     "tab_switches_origin.json",
    //     tabOriginSwitches.asJson
    //   )

    Persistable
      .persistString(
        "tab_switch_graph.json",
        toJsonString(tabGraph)
      )
    // Persistable
    //   .persistString(
    //     "tab_switch_graph_origin.json",
    //     toJsonString(tabOriginGraph)
    //   )

    Persistable
      .persistString(
        "tab_switch_graph.dot",
        toDotString(tabGraph)
      )
    // Persistable
    //   .persistString(
    //     "tab_switch_graph_origin.dot",
    //     toDotString(tabOriginGraph)
    //   )
    Persistable.persistString(
      "tab_switch_graph_clean.dot",
      toDotString(cleanupGraph(tabGraph))
    )
    // Persistable.persistString(
    //   "tab_switch_graph_origin_clean.dot",
    //   toDotString(cleanupGraph(tabOriginGraph))
    // )
  }

  def restore: Try[Unit] = Try {
    Persistable
      .restoreJson("tab_hashes.json")
      .map(decode[Map[String, String]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabHashes = restoredMap
      }

    // Persistable
    //   .restoreJson("tab_hashes_origin.json")
    //   .map(decode[Map[String, String]])
    //   .foreach {
    //     case Left(value)        =>
    //     case Right(restoredMap) => tabOriginHashes = restoredMap
    //   }

    Persistable
      .restoreJson("tab_switches.json")
      .map(decode[Map[String, Map[String, Int]]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabSwitches = restoredMap
      }

    // Persistable
    //   .restoreJson("tab_switches_origin.json")
    //   .map(decode[Map[String, Map[String, Int]]])
    //   .foreach {
    //     case Left(value)        =>
    //     case Right(restoredMap) => tabOriginSwitches = restoredMap
    //   }

    Persistable
      .restoreJson("tab_switch_graph.json")
      .map(TabSwitches.fromJsonString)
      .foreach(tabGraph = _)

    // Persistable
    //   .restoreJson("tab_switch_graph_origin.json")
    //   .map(TabSwitches.fromJsonString)
    //   .foreach(tabOriginGraph = _)
  }

}
