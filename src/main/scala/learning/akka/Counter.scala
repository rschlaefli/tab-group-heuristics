package learning.akka

import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props

object Counter extends App {

  // DOMAIN of the counter actor
  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }

  class CounterActor extends Actor {
    import CounterActor._

    var count = 0
    override def receive: Actor.Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print     => println(count)
    }
  }

  val system = ActorSystem("CounterSystem")

  val counterActor = system.actorOf(Props[CounterActor], "counterActor")

  // these are async!
  import CounterActor._
  counterActor ! Increment
  counterActor ! Increment
  counterActor ! Print
  counterActor ! Decrement
  counterActor ! Print
}
