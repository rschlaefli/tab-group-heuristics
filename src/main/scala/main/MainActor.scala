package main

import java.io.BufferedOutputStream

import scala.concurrent.duration._
import scala.language.postfixOps

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.Timers
import heuristics.HeuristicsAction
import heuristics.HeuristicsActor
import main.Main
import messaging.NativeMessaging
import statistics.StatisticsActor
import tabstate.TabEvent
import tabstate.TabStateActor
import tabswitches.SwitchMapActor

class MainActor extends Actor with ActorLogging with Timers {

  import MainActor._

  implicit val stdout = new BufferedOutputStream(System.out)
  val tabState = context.actorOf(Props[TabStateActor], "TabState")

  override def preStart(): Unit = {
    self ! MainActor.StartProcessing
    timers.startTimerWithFixedDelay("backup", PersistState, 1 minute)
  }

  override def postStop(): Unit = {
    self ! MainActor.StopProcessing
  }

  override def receive: Actor.Receive = {

    case PersistState => {
      context.actorSelection("/user/Main/Heuristics/TabSwitches/TabSwitchMap") ! SwitchMapActor.PersistState
      context.actorSelection("/user/Main/Statistics") ! StatisticsActor.PersistState
    }

    // forward tab events to the tab state processor
    case event: TabEvent => {
      tabState ! event
      sender() ! Main.StreamAck
    }

    case Main.StreamInit =>
      log.info("Stream initialized")
      sender() ! Main.StreamAck

    case Main.StreamComplete =>
      log.info("Stream complete")
      context.stop(self)

    case Main.StreamFail(ex) =>
      log.warning(s"Stream failed with $ex")
      context.stop(self)

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

      context.system.scheduler.scheduleOnce(5 seconds) {
        NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_TABS)
      }(context.system.dispatcher)

      context.system.scheduler.scheduleOnce(5.5 seconds) {
        NativeMessaging.writeNativeMessage(HeuristicsAction.QUERY_GROUPS)
      }(context.system.dispatcher)
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
  case object QueryCurrentState
  case object PersistState

  case object StopProcessing
  case object StartProcessing
}
