package learning.akka

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props

object ActorCapabilities extends App {

  case class SpecialMessage(content: String)

  class SimpleActor extends Actor {
    // context.self -> actor reference
    override def receive: Actor.Receive = {
      case "Hi"                           => context.sender() ! "Hello there"
      case message: String                => println(s"[${context.self.path}] $message")
      case number: Int                    => println(number)
      case SpecialMessage(content)        => println(content)
      case SendMessageToYourself(content) => context.self ! content
      case SayHiTo(actor)                 => actor ! "Hi"
      case PhoneMessage(content, ref)     => ref forward content
    }
  }

  val system = ActorSystem("actorCapabilities")

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  // messages can be of any type
  simpleActor ! "hello"
  simpleActor ! 5
  simpleActor ! SpecialMessage("hey")

  // actors have information about their context
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("hehe")

  // actors can reply to messages
  val alice = system.actorOf(Props[SimpleActor], "alice")
  val bob = system.actorOf(Props[SimpleActor], "bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)

  // if there is no sender (e.g., top scope) -> dead letters (garbage pool)
  alice ! "Hi"

  // forwarding messages
  // D -> A -> B
  // forwarding: sending a message with the original sender
  case class PhoneMessage(content: String, ref: ActorRef)
  alice ! PhoneMessage("Hey", bob)
}
