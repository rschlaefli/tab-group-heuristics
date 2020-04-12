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
import scala.collection.mutable.Queue

import util._
import tabstate._
import heuristics._
import messaging._

object Main extends App with LazyLogging {

  var in: InputStream = null
  var out: OutputStream = null

  try {
    System.in.available()
    in = new BufferedInputStream(System.in)
    out = new BufferedOutputStream(System.out)
  } catch {
    case ioException: IOException => {
      // TODO: send a message to the browser (IO unavailable)

      // if the input channel is unavailable, log an error and exit the program
      logger.error(ioException.getMessage())
      System.exit(1)
    }
  }

  logger.info("> Bootstrapping tab grouping heuristics")

  // initialize the tab state and update queue
  val tabEventsQueue = new Queue[TabEvent](50)

  // TODO: let the tab extension know that heuristics are ready
  // Utils.writeNativeMessage(
  //   out,
  //   TabAction("NOTIFY", NotifyAction("hello from heuristics!").asJson).asJson
  //     .toString()
  // )

  logger.info("> Starting threads")

  // setup a continuous iterator for native message retrieval
  val nativeMessagingThread = NativeMessaging.listen(in, tabEventsQueue)

  // setup a continuous iterator for event processing
  val tabStateThread = TabState.processQueue(tabEventsQueue)

  tabStateThread.start()
  nativeMessagingThread.start()

  logger.info(s"> Daemons started (${Thread.activeCount()})")

  // setup a continually running
  val heuristicsThread = HeuristicsEngine.observe(TabState.currentTabs)

  heuristicsThread.join()
}
