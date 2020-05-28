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

object Main extends App with LazyLogging {

  val io = IO()

  logger.info("> Bootstrapping tab grouping heuristics")

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

  tabStateThread.start()
  nativeMessagingThread.start()
  persistenceThread.start()
  statisticsThread.start()

  logger.info(s"> Daemons started (${Thread.activeCount()})")

  // add a shutdown hook that persists data upon shutdown
  sys.addShutdownHook({
    logger.info("> Shutting down...")
    persistenceThread.interrupt()
    tabStateThread.interrupt()
    nativeMessagingThread.interrupt()
    statisticsThread.interrupt()
    PersistenceEngine.persistCurrentState
    System.exit(143)
  })

  // setup a continually running
  val heuristicsThread = HeuristicsEngine()

  heuristicsThread.join()
}
