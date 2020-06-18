package learning.akka

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props

object Playground extends App {

  // setup an actor system (usually one per application)
  val actorSystem = ActorSystem("HelloAkka")
  println(actorSystem.name)

  // create actors
  // word count actor
  class WordCountActor extends Actor {
    var totalWords = 0
    def receive: Receive = {
      case message: String =>
        totalWords += message.split(" ").length
        println(s"[WordCountActor] I have received $message ($totalWords)")
      case msg =>
        println(s"[WordCountActor] I cannot understand ${msg.toString()}")
    }
  }

  // instantiate actors
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")

  // communication with the actor
  wordCounter ! "I am learning Akka"

}
