package refactor

import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import scala.concurrent.duration._
import akka.util.Timeout
import scala.util.Success
import scala.language.postfixOps

import refactor.CurrentTabsActor._

class CurrentTabsActorSpec
    extends TestKit(ActorSystem("CurrentTabsActorSystem"))
    with ImplicitSender
    with WordSpecLike
    with BeforeAndAfterAll {

  import CurrentTabsActorSpec._

  implicit val timeout = Timeout(1 second)
  implicit val executionContext = system.dispatcher

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "CurrentTabsActor" should {
    val currentTabs = system.actorOf(Props[CurrentTabsActor])

    "return an empty set of current tabs on initial query" in {
      currentTabs ! QueryTabs
      expectMsg(CurrentTabs(List()))
    }

    "initialize the set of current tabs" in {
      currentTabs ! InitializeTabs(List(TabFixtures.Tab1, TabFixtures.Tab2))
      currentTabs ! QueryTabs
      val result =
        expectMsg(CurrentTabs(List(TabFixtures.Tab1, TabFixtures.Tab2)))
      println("CURRENT", result)
    }

    "activate a tab" in {
      currentTabs ! ActivateTab(1, 0)
      expectMsg(TabActivated)

      currentTabs ! QueryActiveTab
      val result = expectMsg(
        ActiveTab(
          Some(TabFixtures.Tab2),
          TabFixtures.Tab2.id,
          TabFixtures.Tab2.windowId
        )
      )
      println("ACTIVATE", result)
    }

    "update the active tab" in {
      currentTabs ! UpdateTab(TabFixtures.Tab2Updated)
      currentTabs ! QueryActiveTab
      val result = expectMsg(ActiveTab(Some(TabFixtures.Tab2Updated), 1, 0))
      println("UPDATE", result)
    }

    "activate a non-existent tab" in {
      currentTabs ! ActivateTab(2, 0)
      currentTabs ! QueryActiveTab
      val result1 = expectMsg(ActiveTab(Some(TabFixtures.Tab2Updated), 1, 0))
      println("ACTIVE", result1)

      currentTabs ! UpdateTab(TabFixtures.Tab3)
      currentTabs ! QueryTabs
      val result2 = expectMsg(
        CurrentTabs(
          List(TabFixtures.Tab1, TabFixtures.Tab2Updated, TabFixtures.Tab3)
        )
      )
      println("CURRENT", result2)

      // TODO: akka paradigm?
      Thread.sleep(500)
      expectMsg(TabActivated(TabFixtures.Tab3))

      currentTabs ! QueryActiveTab
      val result3 = expectMsg(ActiveTab(Some(TabFixtures.Tab3), 2, 0))
      println("ACTIVE", result3)
    }

    "remove an existing tab" in {
      currentTabs ! RemoveTab(0)
      currentTabs ! QueryTabs
      val result =
        expectMsg(CurrentTabs(List(TabFixtures.Tab2Updated, TabFixtures.Tab3)))
      println("CURRENT", result)
    }

  }

}

object CurrentTabsActorSpec {}
