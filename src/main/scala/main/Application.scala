package main

import akka.actor.ActorSystem
import messaging.NativeMessagingActor
import akka.actor.Props
import tabstate.TabStateActor
import statistics.StatisticsActor

object Application extends App {

  println("Bootstrapping application")

  val system = ActorSystem("Application")

  val nativeMessagingActor =
    system.actorOf(Props[NativeMessagingActor], "NativeMessaging")
  val tabStateActor = system.actorOf(Props[TabStateActor], "TabState")
  val statisticsActor = system.actorOf(Props[StatisticsActor], "Statistics")
}
