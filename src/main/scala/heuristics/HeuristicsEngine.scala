package heuristics

import com.typesafe.scalalogging.LazyLogging
import scalax.collection.mutable.Graph
import scalax.collection.edge.WDiEdge

import tabstate._

object HeuristicsEngine extends LazyLogging {
  def apply(): Thread = {
    val thread = new Thread {
      logger.info("> Starting to observe current tab state")
      while (true) {
        // logger.debug(
        //   s"> Current tab state: ${TabState.activeTab}, ${TabState.activeWindow}, ${TabState.currentTabs}, ${TabSwitches.tabOriginGraph}"
        // )

        // logger.info(
        //   s"> Strong components: ${TabSwitches.tabBaseGraph.strongComponentTraverser().map(_.toString())}"
        // )

        TabSwitches
          .extractStrongComponents(
            TabSwitches.cleanupGraph(TabSwitches.tabBaseGraph)
          )
          .map(TabSwitches.toDotString(_))
          .foreach(comp =>
            logger.debug(
              s">> Strong component: ${comp}"
            )
          )

        Thread.sleep(30000)
      }
    }

    thread.setName("HeuristicsEngine")

    thread
  }
}
