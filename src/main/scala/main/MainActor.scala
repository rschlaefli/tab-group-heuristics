package main

import akka.actor.Actor
import akka.actor.ActorLogging
import main.Main.StreamInit
import main.Main.StreamAck
import main.Main.StreamComplete
import main.Main.StreamFail
import tabstate.TabStateActor
import heuristics.HeuristicsActor
import statistics.StatisticsActor
import akka.actor.Props
import tabstate.TabEvent
import akka.actor.PoisonPill
import heuristics.HeuristicsAction
import java.io.BufferedOutputStream
import messaging.NativeMessaging

class MainActor extends Actor with ActorLogging {

  import MainActor._

  implicit val stdout = new BufferedOutputStream(System.out)
  val tabState = context.actorOf(Props[TabStateActor], "TabState")

  override def preStart(): Unit = {
    self ! StartProcessing
  }

  override def receive: Actor.Receive = {

    // forward tab events to the tab state processor
    case event: TabEvent => {
      tabState ! event
      sender() ! StreamAck
    }

    case StreamInit =>
      log.info("Stream initialized")
      sender() ! StreamAck

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
