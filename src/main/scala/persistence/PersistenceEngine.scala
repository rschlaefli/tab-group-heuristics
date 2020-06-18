package persistence

import com.typesafe.scalalogging.LazyLogging

import graph.TabSwitchMap

object PersistenceEngine extends LazyLogging {

  def apply(): Thread = {
    val persistenceThread = new Thread(() => {
      logger.info("> Starting to persist state")

      while (true) {
        Thread.sleep(30000)
        persistCurrentState
      }
    })

    persistenceThread.setName("Persistence")
    persistenceThread.setDaemon(true)
    persistenceThread
  }

  def restoreInitialState = {
    logger.info("> Restoring initial state")

    TabSwitchMap.restore
  }

  def persistCurrentState = {
    logger.info("> Persisting current state")

    TabSwitchMap.persist
  }
}
