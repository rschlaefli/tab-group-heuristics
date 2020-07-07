package tabswitches

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Failure
import scala.util.Try

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
import org.joda.time.DateTime
import persistence.Persistence

case class GraphGenerationParams(
    /**
      * Ignore edges with a lower weight
      */
    minWeight: Double = 2,
    /**
      * Forgetting factor
      */
    expireAfter: Duration = 14 days,
    /**
      * Factor to punish switches on the same origin
      */
    sameOriginFactor: Double = 0.3,
    /**
      * Factor to punish similar URLs
      */
    urlSimilarityFactor: Double = 0.5
)

class SwitchGraphActor extends Actor with ActorLogging {

  import SwitchGraphActor._

  implicit val executionContext = context.dispatcher

  override def receive: Actor.Receive = {

    case ComputeGraph => {
      implicit val timeout = Timeout(2 seconds)
      val switchMap = context.actorSelection(
        "/user/Main/Heuristics/TabSwitches/TabSwitchMap"
      )
      (switchMap ? SwitchMapActor.QueryTabSwitchMap)
        .mapTo[SwitchGraphActor.CurrentSwitchMap]
        .map {
          case SwitchGraphActor.CurrentSwitchMap(tabSwitchMap) => {
            log.debug(
              s"Constructing tab switch graph from switch map with ${tabSwitchMap.size} entries"
            )

            val tabSwitchGraph = processSwitchMap(tabSwitchMap)

            log.debug(
              s"Contructed tab switch graph with ${tabSwitchGraph.vertexSet().size()}" +
                s"nodes and ${tabSwitchGraph.edgeSet().size()} edges"
            )

            context.actorSelection(
              "/user/Main/Heuristics/TabSwitches/TabSwitchGraph"
            ) ! SwitchGraphActor.ExportGraph(tabSwitchGraph)

            TabSwitchActor.CurrentSwitchGraph(tabSwitchGraph)
          }
        }
        .pipeTo(sender())
    }

    case ExportGraph(graph) => {
      val dotString = GraphExport.toDot(graph)
      val csvString = GraphExport.toCsv(graph)
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

  def processSwitchMap(
      tabSwitchMap: Map[String, TabSwitchMeta],
      params: GraphGenerationParams = GraphGenerationParams()
  ): TabSwitchGraph = {

    val tabGraph =
      new SimpleWeightedGraph[TabMeta, DefaultWeightedEdge](
        classOf[DefaultWeightedEdge]
      )

    val expirationFrontier =
      DateTime.now().getMillis() - params.expireAfter.toMillis

    tabSwitchMap.values
      .filter(switchData =>
        switchData.lastUsed >= expirationFrontier
          && switchData.count >= params.minWeight
          && switchData.tab1.url != switchData.tab2.url
        // simply ignore tab switches that were discarded
          && !switchData.wasDiscarded.getOrElse(false)
      )
      .map((switchData: TabSwitchMeta) =>
        Try {
          tabGraph.addVertex(switchData.tab1)
          tabGraph.addVertex(switchData.tab2)
          tabGraph.addEdge(switchData.tab1, switchData.tab2)

          val weightingFactor =
            (switchData.sameOrigin, switchData.urlSimilarity) match {
              case (Some(true), Some(urlSimilarity)) =>
                (1 - params.sameOriginFactor) * (1 - params.urlSimilarityFactor * urlSimilarity)
              case (Some(true), _) =>
                1 - params.sameOriginFactor
              case (_, Some(urlSimilarity)) =>
                1 - params.urlSimilarityFactor * urlSimilarity
              case _ =>
                1
            }

          tabGraph
            .setEdgeWeight(
              switchData.tab1,
              switchData.tab2,
              switchData.count * weightingFactor
            )

        }
      )
      .filter(_.isFailure)
      .foreach {
        case Failure(ex) => logger.error(ex.getMessage())
        case _           =>
      }

    tabGraph
  }
}
