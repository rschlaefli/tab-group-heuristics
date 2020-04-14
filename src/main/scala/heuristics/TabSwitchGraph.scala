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

import tabstate.{Tabs, Tab, TabState}

object TabSwitchGraph extends LazyLogging {

  val jsonTabDescriptor = new NodeDescriptor[Tab](typeId = "Tabs") {
    def id(node: Any) = node match {
      case Tab(
          origin,
          originHash,
          baseHash,
          baseUrl,
          active,
          id,
          index,
          lastAccessed,
          openerTabId,
          pinned,
          sessionId,
          successorTabId,
          title,
          url,
          windowId
          ) =>
        baseHash
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
                List(DotAttr("weight", weight.toString))
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

}
