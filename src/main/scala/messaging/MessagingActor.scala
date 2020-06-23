package messaging

import akka.actor.Actor
import akka.actor.ActorLogging
import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import akka.actor.PoisonPill
import com.typesafe.scalalogging.LazyLogging
import scala.concurrent.Future
import io.circe.syntax._

import heuristics.HeuristicsAction

class MessagingActor extends Actor with ActorLogging {

  import MessagingActor._

  var in: InputStream = null
  var out: OutputStream = null

  override def preStart(): Unit = {
    try {
      System.in.available()
      in = new BufferedInputStream(System.in)
      out = new BufferedOutputStream(System.out)

      log.info(s"Initialized IO to $in/$out")
    } catch {
      case ioException: IOException => {
        // TODO: send a message to the browser (IO unavailable)

        // if the input channel is unavailable, log an error and exit the program
        log.error(ioException.getMessage())
        self ! PoisonPill
      }
    }
  }

  override def receive: Actor.Receive = {
    case SendMessage(action) => writeNativeMessage(out, action)

    case message =>
  }

}

object MessagingActor extends LazyLogging {

  import scala.concurrent.ExecutionContext.Implicits.global

  final case class SendMessage(action: HeuristicsAction)

  def intToByteArray(int: Int): Array[Byte] = {
    List[Byte](
      asByte(int >> 0),
      asByte(int >> 8),
      asByte(int >> 16),
      asByte(int >> 24)
    ).toArray
  }

  def asByte(value: Int): Byte = {
    (value & 0xFF).toByte
  }

  def writeNativeMessage(
      out: OutputStream,
      heuristicsAction: HeuristicsAction
  ): Future[Unit] = Future {
    // encode the action in a json string
    val message = heuristicsAction.asJson.toString()

    // convert the message to a byte array
    val msgBytes = message.getBytes()

    // store the length of the message as a byte array
    val msgLength = msgBytes.length
    val lengthBytes = intToByteArray(msgLength)
    logger.debug(
      s"Pushing action ${heuristicsAction.action} with message length $msgLength"
    )

    // write the message to stdout
    out.write(lengthBytes)
    out.write(msgBytes)
    out.flush()
  }
}
