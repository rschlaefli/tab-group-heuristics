/*
  see https://github.com/rschlaefli/jfreesteel/blob/master/eidnativemessaging/src/main/java/net/devbase/jfreesteel/nativemessaging/EidWebExtensionApp.java
 */

package main

import java.io.{PrintWriter, File}
import java.io.IOException
import java.io.InputStream
import com.typesafe.scalalogging.LazyLogging
import java.io.BufferedOutputStream
import java.io.BufferedInputStream
import scala.util.{Either, Try, Success, Failure}
import java.io.OutputStream
import scala.collection.mutable.{Map, Queue}
import io.circe.Json

import util._
import tabstate._
import heuristics._
import messaging._
import persistence._
import statistics._
import java.net.ServerSocket

object Main extends App with LazyLogging {

  // wait for 10 seconds before cootstrapping
  // this helps ensure that the previous instance has shut down when reloading
  logger.info("> Bootstrapping tab grouping heuristics")
  Thread.sleep(10000)

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

  val io = IO()

  // read persisted state and initialize
  PersistenceEngine.restoreInitialState

  logger.info("> Starting threads")

  // setup a continuous iterator for native message retrieval
  val nativeMessagingThread = NativeMessaging(IO.in, TabState.tabEventsQueue)

  // setup a continuous iterator for event processing
  val tabStateThread = TabState()

  // setup a thread for the persistence engine
  val persistenceThread = PersistenceEngine()

  // setup a thread for the statistics collector
  val statisticsThread = StatisticsEngine()

  // setup a thread that regularly requests a tab group update
  val tabGroupUpdateThread = new Thread(() => {
    while (true) {
      Thread.sleep(120000)

      // refresh the manual tab groups for the next iteration
      NativeMessaging
        .writeNativeMessage(IO.out, HeuristicsAction.QUERY_GROUPS)
    }
  })
  tabGroupUpdateThread.setName("TabGroupUpdates")
  tabGroupUpdateThread.setDaemon(true)

  tabStateThread.start()
  nativeMessagingThread.start()
  persistenceThread.start()
  statisticsThread.start()
  tabGroupUpdateThread.start()

  logger.info(s"> Daemons started (${Thread.activeCount()})")
  NativeMessaging
    .writeNativeMessage(
      IO.out,
      HeuristicsAction.HEURISTICS_STATUS("RUNNING")
    )

  def shutdown = {
    logger.info("> Shutting down...")
    serverSocket.close()
    PersistenceEngine.persistCurrentState
    NativeMessaging
      .writeNativeMessage(
        IO.out,
        HeuristicsAction.HEURISTICS_STATUS("STOPPED")
      )
    System.exit(0)
  }

  // add a shutdown hook that persists data upon shutdown
  sys.addShutdownHook(shutdown)

  // setup a continually running
  val heuristicsThread = HeuristicsEngine()

  heuristicsThread.join()

}
