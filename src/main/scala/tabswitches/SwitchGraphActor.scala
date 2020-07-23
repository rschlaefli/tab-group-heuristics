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
import persistence.Persistence

class SwitchGraphActor extends Actor with ActorLogging {

  import SwitchGraphActor._

  implicit val executionContext = context.dispatcher

  override def receive: Actor.Receive = {

    case ComputeGraph(params) => {
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

            val tabSwitchGraph = processSwitchMap(tabSwitchMap, params)

            log.debug(
              s"Contructed tab switch graph with ${tabSwitchGraph.vertexSet().size()}" +
                s" nodes and ${tabSwitchGraph.edgeSet().size()} edges"
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

  case class ComputeGraph(params: GraphGenerationParams)

  case class CurrentSwitchMap(switchMap: Map[String, TabSwitchMeta])
  case class ExportGraph(graph: TabSwitchGraph)

  def processSwitchMap(
      tabSwitchMap: Map[String, TabSwitchMeta],
      params: GraphGenerationParams
  ): TabSwitchGraph = {

    val tabGraph = new SimpleWeightedGraph[TabMeta, DefaultWeightedEdge](
      classOf[DefaultWeightedEdge]
    )

    val now = java.time.Instant.now().getEpochSecond()

    tabSwitchMap.values
      .filter(switch =>
        switch.tab1 != switch.tab2 && switch.count >= params.minWeight
      )
      .map((switch: TabSwitchMeta) =>
        Try {
          // compute a decay factor depending on the recency of the switch
          val decayFactor = computeDecayFactor(
            now,
            switch.lastUsed,
            (params.expireAfter days).toMillis
          )

          // compute a weighting factor depending on the similarity of tabs in the switch
          val weightingFactor = computeWeightingFactor(
            switch.sameOrigin,
            switch.urlSimilarity,
            params.urlSimilarityFactor,
            0.95
          )

          // TODO: evaluate setting discarded edges to -1
          // TODO: that would require filtering these edges before pagerank
          val edgeWeight =
            if (switch.wasDiscarded.getOrElse(false)) 0
            else switch.count * weightingFactor * decayFactor

          if (edgeWeight != 0) {
            tabGraph.addVertex(switch.tab1)
            tabGraph.addVertex(switch.tab2)
            tabGraph.addEdge(switch.tab1, switch.tab2)
            tabGraph.setEdgeWeight(switch.tab1, switch.tab2, edgeWeight)
          }
        }
      )
      .filter(_.isFailure)
      .foreach {
        case Failure(ex) => logger.error(ex.getMessage())
        case _           =>
      }

    tabGraph
  }

  def computeDecayFactor(
      nowTs: Long,
      itemTs: Long,
      expiration: Long
  ): Double = {

    val expirationFrontier = nowTs - expiration

    if (itemTs < expirationFrontier) {
      0
    } else {
      (itemTs - expirationFrontier) / (nowTs - expirationFrontier)
    }

  }

  def computeWeightingFactor(
      sameOrigin: Option[Boolean],
      urlSimilarity: Option[Float],
      similarityWeight: Double,
      maxSimilarity: Double
  ): Double = (sameOrigin, urlSimilarity) match {
    // if the urls in the switch are too similar, ignore it completely
    case (Some(true), Some(urlSimilarity)) if urlSimilarity > maxSimilarity =>
      0
    // if the urls are similar, reduce the weight of the switch
    case (_, Some(urlSimilarity)) =>
      1 - similarityWeight * urlSimilarity
    // a normal switch should be weighted normally
    case _ =>
      1
  }
}
