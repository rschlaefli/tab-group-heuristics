package learning

object Curries extends App {
  def curriedAdder(x: Int)(y: Int): Int = x + y

  val add4: Int => Int = curriedAdder(4)

  // lifting of curriedAdder (def = method) to a function type
  // functions != methods, as methods always need all param lists
  def inc(x: Int) = x + 1
  List(1, 2, 3).map(x => inc(x))

  // underscores are powerful
  def concatenator(x: String, y: String, z: String): String = x + y + z

  // creates a function (x, y) => concatenator("Hello ", x, y)
  val fillInTheBlanks = concatenator("Hello ", _: String, _: String)
}

object ByNameAndFunction extends App {
  def byName(n: => Int) = n + 1
  def byFunction(f: () => Int) = f() + 1

  def method = 42 // parameter-less method
  def parenMethod() = 42

  // byName takes a function as a param and
  // executes that function to get to a value
  byName(23)
  byName(method)
  byName(parenMethod)
  // byName(() => 42)
  byName((() => 42)())
  // byName(parenMethod 42)

  // byFunction(42)
  // byFunction(method) // does not take parameterless methods
  byFunction(parenMethod)
}
