/*
  see https://github.com/rschlaefli/jfreesteel/blob/master/eidnativemessaging/src/main/java/net/devbase/jfreesteel/nativemessaging/EidWebExtensionApp.java
 */

package collector

import java.io.{PrintWriter, File}
import java.io.IOException
import java.io.InputStream
import com.typesafe.scalalogging.LazyLogging
import java.io.BufferedOutputStream
import java.io.BufferedInputStream
import scala.util.{Either, Try, Success, Failure}
import java.io.OutputStream
import scala.collection.mutable.Queue

import events._
import tabstate._
import heuristics._

object Collector extends App with LazyLogging {

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

  // initialize the tab state and update queue
  val tabEventsQueue = new Queue[TabEvent](50)

  // TODO: let the tab extension know that heuristics are ready
  // Utils.writeNativeMessage(
  //   out,
  //   TabAction("NOTIFY", NotifyAction("hello from heuristics!").asJson).asJson
  //     .toString()
  // )

  // setup a continuous iterator for native message retrieval
  val messageReceiver = new Thread(() =>
    Iterator
      .continually(Utils.readNativeMessage(in))
      // flatten to get rid of any null values (None)
      .flatten
      // try to decode the messages into tab events
      .flatMap(TabEvent.decodeEventFromMessage)
      // add all the processed tab events to the queue
      .foreach(tabEvent => {
        logger.debug(s"About to add $tabEvent to the queue")
        tabEventsQueue.synchronized {
          tabEventsQueue.enqueue(tabEvent)
          tabEventsQueue.notify()
        }
        logger.debug(s"Queue contains ${tabEventsQueue.size}")
      })
  )

  // setup a continuous iterator for event processing
  val eventProcesser = TabState.processQueue(tabEventsQueue)

  // setup a continually running
  val heuristicsEngine = HeuristicsEngine.observe(internalState)

  messageReceiver.setName("MessageReceiver")
  eventProcesser.setName("EventProcessor")
  heuristicsEngine.setName("HeuristicsEngine")

  // set the message and event processor threads to be daemons
  // TODO: this would make sense, but does not work as is
  messageReceiver.setDaemon(true)
  eventProcesser.setDaemon(true)

  messageReceiver.start()
  eventProcesser.start()
  heuristicsEngine.start()
}
