package persistence

import com.typesafe.scalalogging.LazyLogging

import heuristics.TabSwitches

object PersistenceEngine extends LazyLogging {

  def apply(): Thread = {
    val persistenceThread = new Thread(() => {
      logger.info("> Starting to persist state")

      while (true) {
        TabSwitches.persist
        Thread.sleep(60000)
      }
    })

    persistenceThread.setName("Persistence")
    persistenceThread.setDaemon(true)

    persistenceThread
  }

  def restoreInitialState = {
    TabSwitches.restore
  }

}
