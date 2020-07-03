package main

import java.io.BufferedOutputStream

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.PoisonPill
import akka.actor.Props
import heuristics.HeuristicsAction
import heuristics.HeuristicsActor
import main.Main.StreamAck
import main.Main.StreamComplete
import main.Main.StreamFail
import main.Main.StreamInit
import messaging.NativeMessaging
import statistics.StatisticsActor
import tabstate.TabEvent
import tabstate.TabStateActor

class MainActor extends Actor with ActorLogging {

  import MainActor._

  implicit val stdout = new BufferedOutputStream(System.out)
  val tabState = context.actorOf(Props[TabStateActor], "TabState")

  override def preStart(): Unit = {
    self ! MainActor.StartProcessing
  }

  override def receive: Actor.Receive = {

    // forward tab events to the tab state processor
    case event: TabEvent => {
      tabState ! event
      sender() ! Main.StreamAck
    }

    case StreamInit =>
      log.info("Stream initialized")
      sender() ! Main.StreamAck

    case StreamComplete =>
      log.info("Stream complete")
      context.stop(self)

    case StreamFail(ex) =>
      log.warning(s"Stream failed with $ex")

    case StartProcessing => {
      log.info(s"Starting processing ${context.children}")

      if (context.child("Heuristics").isEmpty) {
        context.actorOf(Props[HeuristicsActor], "Heuristics")
      }

      if (context.child("Statistics").isEmpty) {
        context.actorOf(Props[StatisticsActor], "Statistics")
      }

      NativeMessaging.writeNativeMessage(
        HeuristicsAction.HEURISTICS_STATUS("RUNNING")
      )

      NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_GROUPS)
    }

    case StopProcessing => {
      val heuristics = context.child("Heuristics")
      if (heuristics.isDefined) heuristics.get ! PoisonPill

      val statistics = context.child("Statistics")
      if (statistics.isDefined) statistics.get ! PoisonPill

      NativeMessaging.writeNativeMessage(
        HeuristicsAction.HEURISTICS_STATUS("STOPPED")
      )
    }

  }

}

object MainActor {
  case object StopProcessing
  case object StartProcessing
}
