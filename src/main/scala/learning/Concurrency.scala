package learning

object Intro extends App {

  // JVM threads
  val aThread = new Thread {
    override def run(): Unit = println("running in parallel")
  }

  // signal the JVM to start the thread
  aThread.start()

  // block until the thread has finished running
  aThread.join()

  // synchronized
  // no two threads can evaluate the expression at the same time

  // @volatile
  // all reads and writes to the value will be synchronized
}
