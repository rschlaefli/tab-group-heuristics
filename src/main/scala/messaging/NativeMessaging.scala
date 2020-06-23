package messaging

import com.typesafe.scalalogging.LazyLogging
import io.circe.syntax._
import java.io.OutputStream
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import heuristics.HeuristicsAction

object NativeMessaging extends LazyLogging {

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
      heuristicsAction: HeuristicsAction
  )(implicit out: OutputStream): Future[Unit] = Future {
    // encode the action in a json string
    val message = heuristicsAction.asJson.toString()

    // convert the message to a byte array
    val msgBytes = message.getBytes()

    // store the length of the message as a byte array
    val msgLength = msgBytes.length
    val lengthBytes = intToByteArray(msgLength)
    logger.debug(
      s"> Pushing action ${heuristicsAction.action} with message length $msgLength"
    )

    // write the message to stdout
    out.write(lengthBytes)
    out.write(msgBytes)
    out.flush()
  }

}
