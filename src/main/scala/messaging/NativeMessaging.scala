package messaging

import scala.collection.mutable
import com.typesafe.scalalogging.LazyLogging
import java.io.InputStream
import tabstate.TabEvent

import util.Utils

object NativeMessaging extends LazyLogging {
  def listen(
      in: InputStream,
      tabEventsQueue: mutable.Queue[TabEvent]
  ): Thread = {
    val thread = new Thread(() => {
      logger.info("> Starting to listen for messages")
      Iterator
        .continually(Utils.readNativeMessage(in))
        // flatten to get rid of any null values (None)
        .flatten
        // try to decode the messages into tab events
        .flatMap(TabEvent.decodeEventFromMessage)
        // add all the processed tab events to the queue
        .foreach(tabEvent => {
          logger.debug(
            s"> Processing tab event $tabEvent, current queue size $tabEventsQueue.size"
          )
          tabEventsQueue.synchronized {
            tabEventsQueue.enqueue(tabEvent)
            tabEventsQueue.notify()
          }
        })
    })

    thread.setName("NativeMessaging")
    thread.setDaemon(true)

    thread
  }

}
