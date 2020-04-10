package learning

object Lazy extends App {
  lazy val x: Int = {
    println("executed")
    42
  }

  // lazy executed
  println(x)

  // cached and not executed again (no log)
  println(x)

  def byNameMethod(n: => Int): Int = {
    // call-by-need
    // n is computed exactly once!
    lazy val t = n
    t + t + t + 1
  }

  def retrieveMagicValue: Int = {
    println("waiting")
    Thread.sleep(1000)
    42
  }

  println(byNameMethod(retrieveMagicValue))

  // filtering with lazy vals
  val numbers = List(1, 25, 40, 5, 23)
  // numbers.withFilter(...)

  // for-comprehensions use withFilter with guards
  for {
    a <- List(1, 2, 3) if a % 2 == 0 // use lazy vals!
  } yield a + 1
}
