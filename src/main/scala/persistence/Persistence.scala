package persistence

import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import java.io.PrintWriter
import scala.util.Try

object Persistence extends LazyLogging {
  def checkStorage = {
    // TODO: check the storage for existing tab switching data
    // TODO: return the existing data
  }

  def persistJson(fileName: String, jsonData: Json): Unit = Try {
    Some(new PrintWriter(fileName)).foreach { p =>
      p.write(jsonData.toString()); p.close
    }
  }

  def restoreJson(fileName: String): Try[String] = Try {
    scala.io.Source.fromFile(fileName).getLines.mkString
  }
}
