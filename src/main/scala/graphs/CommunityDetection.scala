package graphs

import scalax.collection.mutable.Graph
import scalax.collection.edge.WDiEdge
import scalax.collection.io.dot._
import scalax.collection.edge.Implicits._

import tabstate.Tabs
import tabstate.Tab
import heuristics.TabSwitches

object CommunityDetection extends App {
  def createFakeTab(origin: String, base: String, tabId: Int, url: String) = {
    Tab(
      origin,
      origin,
      base,
      base,
      true,
      tabId,
      tabId,
      tabId,
      None,
      false,
      Some(1),
      None,
      url,
      url,
      1
    )
  }

  val graph = Graph[Tabs, WDiEdge]()

  val tab1 = createFakeTab("a", "b", 1, "tab1")
  val tab2 = createFakeTab("c", "d", 2, "tab2")
  val tab3 = createFakeTab("e", "f", 3, "tab3")
  val tab4 = createFakeTab("g", "h", 4, "tab4")

  graph += tab1
  graph += tab2
  graph += tab3
  graph += tab4

  graph += new WDiEdge((tab1, tab2), 1)
  graph += new WDiEdge((tab2, tab3), 2)
  graph += new WDiEdge((tab2, tab4), 1)
  graph += new WDiEdge((tab1, tab4), 3)
  graph += new WDiEdge((tab3, tab1), 2)

  println(graph)

  println(graph.toDot(TabSwitches.dotRoot, TabSwitches.edgeTransformer))

  graph.strongComponentTraverser().map(println(_))

}
