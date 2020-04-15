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

  var tabBaseSwitches = Map[String, Map[String, Int]]()
  var tabOriginSwitches = Map[String, Map[String, Int]]()
  var tabBaseHashes = Map[String, String]()
  var tabOriginHashes = Map[String, String]()

  var tabBaseGraph = Graph[Tabs, WDiEdge]()
  var tabOriginGraph = Graph[Tabs, WDiEdge]()

  def cleanupGraph(graph: Graph[Tabs, WDiEdge]): Graph[Tabs, WDiEdge] = {
    // remove all nodes that have a very low incoming weight
    // i.e., remove nodes that have been switched to few times
    graph -- graph.nodes.filter(node =>
      node.incoming.map(edge => edge.weight).sum <= 1
    )
  }

  def processInitialTabs(initialTabs: List[Tab]) = {
    tabBaseHashes ++= initialTabs.map(tab => (tab.baseHash, tab.baseUrl))
    tabOriginHashes ++= initialTabs.map(tab => (tab.originHash, tab.origin))

    tabBaseGraph ++= initialTabs
    tabOriginGraph ++= initialTabs
  }

  /**
    * Process a tab update for the tab switching heuristic
    * Updates the switching and hash maps and the tab switch graph
    *
    * @param previousTab
    * @param currentTab
    */
  def processTabSwitch(previousTab: Option[Tab], currentTab: Tab) {
    tabBaseGraph += currentTab
    tabOriginGraph += currentTab

    tabBaseHashes.update(currentTab.baseHash, currentTab.baseUrl)
    tabOriginHashes.update(currentTab.originHash, currentTab.origin)

    // check if the location of the tab changes in this update
    // if yes, we need to also account for the tab switch
    // TODO: does this make sense or is that update only happening if we leave a tab completely?
    if (previousTab.isDefined) {
      val prevTab = previousTab.get

      if (prevTab.baseHash != currentTab.baseHash) {
        logger.info(
          s"Processing fake tab switch (base) for tab ${currentTab.id}"
        )

        tabBaseSwitches.updateWith(prevTab.baseHash)((switchMap) => {
          val map = switchMap.getOrElse(Map((currentTab.baseHash, 0)))

          val previousCount = map.getOrElse(currentTab.baseHash, 0)

          tabBaseGraph -= WDiEdge((prevTab, currentTab))(previousCount)
          tabBaseGraph += WDiEdge((prevTab, currentTab))(
            previousCount + 1
          )

          map.update(currentTab.baseHash, previousCount + 1)

          Some(map)
        })
      }

      if (prevTab.originHash != currentTab.originHash) {
        logger.info(
          s"Processing fake tab switch (origin) for tab ${currentTab.id}"
        )

        tabOriginSwitches.updateWith(prevTab.originHash)((switchMap) => {
          val map = switchMap
            .getOrElse(Map((currentTab.originHash, 0)))

          val previousCount = map.getOrElse(currentTab.originHash, 0)

          tabOriginGraph -= WDiEdge((prevTab, currentTab))(previousCount)
          tabOriginGraph += WDiEdge((prevTab, currentTab))(
            previousCount + 1
          )

          map.update(currentTab.originHash, previousCount + 1)

          Some(map)
        })
      }
    }
  }

  val jsonTabDescriptor = new NodeDescriptor[Tab](typeId = "Tabs") {
    def id(node: Any) = node match {
      case tab: Tab => tab.baseHash
    }
  }

  val jsonGraphDescriptor = new Descriptor[Tabs](
    defaultNodeDescriptor = jsonTabDescriptor,
    defaultEdgeDescriptor = WDi.descriptor[Tabs]()
  )

  def toJsonString(graph: Graph[Tabs, WDiEdge]): String = {
    graph.toJson(jsonGraphDescriptor)
  }

  def fromJsonString(jsonString: String) = {
    Graph.fromJson[Tabs, WDiEdge](jsonString, jsonGraphDescriptor)
  }

  val dotRoot = DotRootGraph(
    directed = true,
    id = Some("Tabs")
    // attrStmts = List(DotAttrStmt(Elem.node, List(DotAttr("shape", "record")))),
    // attrList = List(DotAttr("attr_1", """"one""""), DotAttr("attr_2", "<two>"))
  )

  def edgeTransformer(
      innerEdge: Graph[Tabs, WDiEdge]#EdgeT
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
  def toDotString(graph: Graph[Tabs, WDiEdge]): String = {
    graph.toDot(dotRoot, edgeTransformer)
  }

  def persist: Try[Unit] = Try {
    Persistable
      .persistJson("tab_hashes_base.json", tabBaseHashes.asJson)
    Persistable
      .persistJson(
        "tab_hashes_origin.json",
        tabOriginHashes.asJson
      )

    Persistable
      .persistJson(
        "tab_switches_base.json",
        tabBaseSwitches.asJson
      )
    Persistable
      .persistJson(
        "tab_switches_origin.json",
        tabOriginSwitches.asJson
      )

    Persistable
      .persistString(
        "tab_switch_graph_base.json",
        toJsonString(tabBaseGraph)
      )
    Persistable
      .persistString(
        "tab_switch_graph_origin.json",
        toJsonString(tabOriginGraph)
      )

    Persistable
      .persistString(
        "tab_switch_graph_base.dot",
        toDotString(tabBaseGraph)
      )
    Persistable
      .persistString(
        "tab_switch_graph_origin.dot",
        toDotString(tabOriginGraph)
      )
    Persistable.persistString(
      "tab_switch_graph_clean.dot",
      toDotString(cleanupGraph(tabBaseGraph))
    )
  }

  def restore: Try[Unit] = Try {
    Persistable
      .restoreJson("tab_hashes_base.json")
      .map(decode[Map[String, String]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabBaseHashes = restoredMap
      }

    Persistable
      .restoreJson("tab_hashes_origin.json")
      .map(decode[Map[String, String]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabOriginHashes = restoredMap
      }

    Persistable
      .restoreJson("tab_switches_base.json")
      .map(decode[Map[String, Map[String, Int]]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabBaseSwitches = restoredMap
      }

    Persistable
      .restoreJson("tab_switches_origin.json")
      .map(decode[Map[String, Map[String, Int]]])
      .foreach {
        case Left(value)        =>
        case Right(restoredMap) => tabOriginSwitches = restoredMap
      }

    Persistable
      .restoreJson("tab_switch_graph_base.json")
      .map(TabSwitches.fromJsonString)
      .foreach(tabBaseGraph = _)

    Persistable
      .restoreJson("tab_switch_graph_origin.json")
      .map(TabSwitches.fromJsonString)
      .foreach(tabOriginGraph = _)
  }

}
