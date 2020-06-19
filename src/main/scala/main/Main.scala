package main

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.StreamConverters
import com.typesafe.scalalogging.LazyLogging
import akka.util.ByteString
import java.net.ServerSocket
import scala.util.{Either, Try, Success, Failure}
import akka.actor.CoordinatedShutdown

import tabstate.TabStateActor
import heuristics.HeuristicsActor
import messaging.IO
import messaging.NativeMessaging
import heuristics.HeuristicsAction
import tabstate.TabEvent

object Main extends App with LazyLogging {

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFail(ex: Throwable)

  logger.info("Bootstrapping application")
  Thread.sleep(5000)

  IO()

  // try to bind to a server socket (to ensure we can only have one instance at a time)
  var serverSocket: ServerSocket = null
  Try { new ServerSocket(12345) } match {
    case Success(socket) => {
      serverSocket = socket
    }
    case Failure(e) => {
      logger.error(
        "> Unable to bind to the socket (there might be another instance running). Exiting..."
      )
      NativeMessaging
        .writeNativeMessage(
          IO.out,
          HeuristicsAction.HEURISTICS_STATUS("ALREADY_RUNNING")
        )
      System.exit(0)
    }
  }

  implicit val system = ActorSystem("Application")
  implicit val materializer = ActorMaterializer()

  // create a stream source from standard input
  // map every incoming message to the content part and decode the contained JSON string
  // the chunksize has to be high as we can get big json payloads from the extension (e.g., for tab groups)
  val source = StreamConverters
    .fromInputStream(() => IO.in, 65536)
    .map(_.drop(4).utf8String)
    .map(TabEvent.decodeEventFromMessage)
    .filter(_.isDefined)
    .map(_.get)

  // setup actors
  val tabState = system.actorOf(Props[TabStateActor], "TabState")
  val heuristics = system.actorOf(Props[HeuristicsActor], "Heuristics")
  // val statisticsActor = system.actorOf(Props[StatisticsActor], "Statistics")

  // create a stream sink for the message processing actor
  val sink = Sink
    .actorRefWithAck[TabEvent](
      tabState,
      onInitMessage = StreamInit,
      onCompleteMessage = StreamComplete,
      ackMessage = StreamAck,
      onFailureMessage = throwable => StreamFail(throwable)
    )

  // start processing incoming events
  val graph = source.to(sink).run()

  logger.info(s"> Daemons started (${Thread.activeCount()})")
  NativeMessaging
    .writeNativeMessage(
      IO.out,
      HeuristicsAction.HEURISTICS_STATUS("RUNNING")
    )

  CoordinatedShutdown(system).addJvmShutdownHook {
    logger.info("Shutting down...")

    serverSocket.close()
    serverSocket = null

    NativeMessaging
      .writeNativeMessage(
        IO.out,
        HeuristicsAction.HEURISTICS_STATUS("STOPPED")
      )

    // TODO: trigger persistence
  }
}
