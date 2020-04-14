package heuristics

import com.typesafe.scalalogging.LazyLogging
import scalax.collection.io.json.descriptor.NodeDescriptor
import scalax.collection.io.json.descriptor.Descriptor
import scalax.collection.io.json.descriptor.predefined.WDi
import scalax.collection.io.json._

import tabstate.{Tabs, Tab, TabState}
import scalax.collection.edge.WDiEdge
import scalax.collection.mutable.Graph

object TabSwitchGraph extends LazyLogging {

  val tabDescriptor = new NodeDescriptor[Tab](typeId = "Tabs") {
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

  val graphDescriptor = new Descriptor[Tabs](
    defaultNodeDescriptor = tabDescriptor,
    defaultEdgeDescriptor = WDi.descriptor[Tabs]()
  )

  def toJsonString: String = {
    TabState.tabOriginGraph.toJson(graphDescriptor)
  }

  def fromJsonString(jsonString: String) = {
    Graph.fromJson[Tabs, WDiEdge](jsonString, graphDescriptor)
  }
}
