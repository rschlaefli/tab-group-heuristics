package util

import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder

object Utils extends LazyLogging {
  def extractDecoderResult[T](
      decoderResult: Decoder.Result[T]
  ): Option[T] = decoderResult match {
    case Left(decodeError) => {
      logger.error(decodeError.message)
      None
    }
    case Right(value) => Some(value)
  }
}
