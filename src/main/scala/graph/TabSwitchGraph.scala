package graph

import com.typesafe.scalalogging.LazyLogging
import scalax.collection.io.json.descriptor.NodeDescriptor
import scalax.collection.io.json.descriptor.Descriptor
import scalax.collection.io.json.descriptor.predefined.WUnDi
import scalax.collection.io.json._
import scalax.collection.edge.WUnDiEdge
import scalax.collection.io.dot._
import scalax.collection.mutable.Graph, scalax.collection.GraphEdge._

import tabstate.Tab
import scalax.collection.io.dot.DotEdgeStmt
import scalax.collection.io.dot.DotGraph
import scalax.collection.io.dot.DotAttr
import scalax.collection.io.dot.DotRootGraph
import org.joda.time.DateTime

object TabSwitchGraph extends LazyLogging {

  var tabSwitches = Map[(String, String), TabSwitchMeta]()

  def +(prevTab: Tab, currentTab: Tab, weight: Int) = {
    // TODO: order prevTab and currentTab hash so that we only get one identifier per pair
    // tabSwitches.updatedWith()
  }

  val jsonTabDescriptor = new NodeDescriptor[Tab](typeId = "Tabs") {
    def id(node: Any) = node match { case tab: Tab => tab.hash }
  }

  val jsonGraphDescriptor = new Descriptor[Tab](
    defaultNodeDescriptor = jsonTabDescriptor,
    defaultEdgeDescriptor = WUnDi.descriptor[Tab]()
  )

  def toJsonString(graph: Graph[Tab, WUnDiEdge]): String = {
    graph.toJson(jsonGraphDescriptor)
  }

  def fromJsonString(jsonString: String) = {
    Graph.fromJson[Tab, WUnDiEdge](jsonString, jsonGraphDescriptor)
  }

  // val dotRoot = DotRootGraph(directed = false, id = Some("Tabs"))

  // def edgeTransformer(
  //     innerEdge: Graph[Tab, WUnDiEdge]#EdgeT
  // ): Option[(DotGraph, DotEdgeStmt)] = innerEdge.edge match {
  //   case WUnDiEdge(source, target, weight) =>
  //     Some(
  //       (
  //         dotRoot,
  //         DotEdgeStmt(
  //           source.toString,
  //           target.toString,
  //           List(
  //             DotAttr("weight", weight.toInt),
  //             DotAttr("label", weight.toInt.toString)
  //           )
  //         )
  //       )
  //     )
  // }
}
