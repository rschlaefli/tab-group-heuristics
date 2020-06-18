package messaging

import java.io.InputStream
import java.io.OutputStream
import java.io.BufferedOutputStream
import java.io.BufferedInputStream
import java.io.IOException
import com.typesafe.scalalogging.LazyLogging

object IO extends LazyLogging {
  var in: InputStream = null
  var out: OutputStream = null

  def apply() = {
    try {
      System.in.available()
      in = new BufferedInputStream(System.in)
      out = new BufferedOutputStream(System.out)

      logger.info(s"Initialized IO to $in/$out")
    } catch {
      case ioException: IOException => {
        // TODO: send a message to the browser (IO unavailable)

        // if the input channel is unavailable, log an error and exit the program
        logger.error(ioException.getMessage())
        System.exit(1)
      }
    }
  }
}
