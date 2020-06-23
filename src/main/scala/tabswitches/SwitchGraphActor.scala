package tabswitches

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import persistence.Persistence

import SwitchMapActor.QueryTabSwitchMap
import TabSwitchActor.CurrentSwitchGraph

class SwitchGraphActor extends Actor with ActorLogging {

  import SwitchGraphActor._

  implicit val executionContext = context.dispatcher

  override def receive: Actor.Receive = {

    case ComputeGraph => {
      implicit val timeout = Timeout(2 seconds)
      val switchMap = context.actorSelection(
        "/user/Heuristics/TabSwitches/TabSwitchMap"
      )
      (switchMap ? QueryTabSwitchMap)
        .mapTo[CurrentSwitchMap]
        .map {
          case CurrentSwitchMap(tabSwitchMap) => {
            log.debug(
              s"Constructing tab switch graph from switch map with ${tabSwitchMap.size} entries"
            )

            val tabSwitchGraph = GraphUtils.processSwitchMap(tabSwitchMap)

            log.debug(
              s"Contructed tab switch graph with ${tabSwitchGraph.vertexSet().size()}" +
                s"nodes and ${tabSwitchGraph.edgeSet().size()} edges"
            )

            context.actorSelection(
              "/user/Heuristics/TabSwitches/TabSwitchGraph"
            ) ! ExportGraph(tabSwitchGraph)

            CurrentSwitchGraph(tabSwitchGraph)
          }
        }
        .pipeTo(sender())
    }

    case ExportGraph(graph) => {
      val dotString = GraphUtils.exportToDot(graph)
      val csvString = GraphUtils.exportToCsv(graph)
      Persistence.persistString("tab_switches.dot", dotString)
      Persistence.persistString("tab_switches.txt", csvString)
    }

    case _ =>
  }

}

object SwitchGraphActor extends LazyLogging {

  import TabSwitchActor.TabSwitchGraph

  case object ComputeGraph

  case class CurrentSwitchMap(switchMap: Map[String, TabSwitchMeta])
  case class ExportGraph(graph: TabSwitchGraph)
}
