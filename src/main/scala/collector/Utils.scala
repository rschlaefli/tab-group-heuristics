package collector

import scala.runtime.RichByte
import java.io.{InputStream, OutputStream}
import com.typesafe.scalalogging.LazyLogging
import java.util.concurrent.ArrayBlockingQueue
import scala.collection.mutable

object Utils extends LazyLogging {
  def readToByteArray(in: InputStream, length: Int): Option[Array[Byte]] = {
    val byteArray = new Array[Byte](length)
    val count = in.read(byteArray)
    if (count == -1) return None
    Some(byteArray)
  }

  def byteArrayToInt(byteArray: Array[Byte]): Int = {
    java.nio.ByteBuffer
      .wrap(byteArray)
      .order(java.nio.ByteOrder.LITTLE_ENDIAN)
      .getInt
  }

  def byteArrayToString(byteArray: Array[Byte]): String = {
    new String(byteArray, "UTF8")
  }

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

  def readNativeMessage(in: InputStream): Option[String] = {
    // read the length of the message
    val message = readToByteArray(in, 4)
      .map(byteArrayToInt)
      // read the actual message based on its length
      .map(readToByteArray(in, _))
      .flatten
      .map(byteArrayToString)

    // TODO: is it any use to sleep here?
    Thread.sleep(100)

    message
  }

  def writeNativeMessage(out: OutputStream, message: String) = {
    // convert the message to a byte array
    val msgBytes = message.getBytes()

    // store the length of the message as a byte array
    val msgLength = msgBytes.length
    val lengthBytes = intToByteArray(msgLength)
    logger.info(s"Writing native message with length $msgLength")

    // write the message to stdout
    out.write(lengthBytes)
    out.write(msgBytes)
    out.flush()
  }

  def dequeueTabEvent(queue: mutable.Queue[TabEvent]): TabEvent = {
    queue.synchronized {
      if (queue.isEmpty) {
        queue.wait()
      }
      return queue.dequeue()
    }
  }
}
