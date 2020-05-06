package graphs

import scalax.collection.Graph
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot._
import scalax.collection.edge.Implicits._

import tabstate.Tabs
import tabstate.Tab
import heuristics.TabSwitches
import com.typesafe.scalalogging.LazyLogging

object CommunityDetection extends App with LazyLogging {
  def processGraph(graph: Graph[Tabs, WDiEdge]) = {
    logger.info("> processing the graph")
  }
}
