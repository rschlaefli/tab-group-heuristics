package statistics

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.actor.Props

class StatisticsActorSpec
    extends TestKit(ActorSystem("StatisticsActorSystem"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll {

  // setup
  import StatisticsActorSpec._
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "StatisticsActor" should {
    "do this" in {
      val echoActor = system.actorOf(Props[StatisticsActor])
      echoActor ! "hello world"

      expectMsg("hello world!")
    }
  }

}

object StatisticsActorSpec {}
