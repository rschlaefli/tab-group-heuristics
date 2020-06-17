package graph

import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime
import scala.collection.mutable
import org.jgrapht.graph._
import org.jgrapht.Graph

import tabstate.Tab

object TabSwitchGraph extends LazyLogging {

  def apply(): Graph[Tab, DefaultWeightedEdge] = {
    val tabSwitchMap = TabSwitchMap.tabSwitches

    var tabGraph =
      new SimpleWeightedGraph[Tab, DefaultWeightedEdge](
        classOf[DefaultWeightedEdge]
      )

    tabGraph
  }

}
