package course

import scala.util.Try
import scala.util.Success

object FP extends App {
  // function traits
  val concatenator = new Function2[String, String, String] {
    def apply(v1: String, v2: String): String = v1 + v2
  }
  println(concatenator("hello ", "world"))

  // anonymous function
  val doubler = (x: Int) => x * 2

  // sugar with _
  val niceDoubler: Int => Int = _ * 2 // equivalent to x => x * 2
  val niceAdder: (Int, Int) => Int = _ + _ // equivalent to (a,b) => a + b

  // curried functions
  val curriedFunction = (a: Int) => (b: Int) => a + b
  println(curriedFunction(1)(2))

  // function that applies a function n times over a value x
  // nTimes(f, n, x)
  def nTimes(f: Int => Int, n: Int, x: Int): Int =
    if (n <= 0) x
    else nTimes(f, n - 1, f(x))

  val plusOne: Int => Int = _ + 1
  println(nTimes(plusOne, 10, 1))

  // returning a lambda
  def nTimesBetter(f: Int => Int, n: Int): (Int => Int) =
    if (n <= 0) (x: Int) => x
    else (x: Int) => nTimesBetter(f, n - 1)(f(x))

  val plus10 = nTimesBetter(plusOne, 10)
  println(plus10(1))

  // functions with multiple parameter lists
  def curriedFormatter(c: String)(x: Double): String = c.format(x)
  val standardFormat: (Double => String) = curriedFormatter("%4.2f")
  val preciseFormat: (Double => String) = curriedFormatter("%10.8f")
  println(standardFormat(Math.PI))
  println(preciseFormat(Math.PI))

  // for-comprehensions
  // rewritten to flatMap and map by the compiler
  val numbers = List(1, 2, 3)
  val chars = List('a', 'b', 'c')
  val colors = List("red", "green")
  val forCombinations = for {
    n <- numbers if n % 2 == 0 // this is a "guard"
    c <- chars
    color <- colors
  } yield "" + c + n + "-" + color
  println(forCombinations)

  // tuples
  val aTuple = (2, "hello")
  println(aTuple._1)
  println(aTuple._2)

  // maps
  val aMap: Map[String, Int] = Map()
  val phonebook = Map(("Jim", 222), ("Daniel", 123))
  println(phonebook)
  println(phonebook.map(pair => pair._1.toLowerCase() -> pair._2))

  // groupby
  val names = List("Roland", "Boland", "Holland", "Robin")
  println(names.groupBy(name => name.charAt(0)))
}

object Options extends App {
  def unsafeMethod(): Option[String] = None
  def backupMethod(): Option[String] = Some("Valid content")
  val chainedResult = unsafeMethod() orElse backupMethod()
  println(chainedResult)

  val someOption: Option[Int] = Some(10)
  println(someOption.map(_ * 2))
  println(someOption.map(_ * 3).filter(_ > 20))
  println(someOption.flatMap(x => Option(x * 10)))
}

object Failure extends App {
  // try objects
  def unsafeMethod(): String = throw new RuntimeException("NO STRING HERE!")
  def backupMethod(): Try[String] = Success("Here is a string!")
  val potentialFailure: Try[String] = Try(unsafeMethod())
  println(potentialFailure)

  // syntax sugar
  val anotherPotentialFailure = Try {
    // code that might fail
  }

  // utilities
  println(potentialFailure.isSuccess)
  println(potentialFailure.isFailure)

  // fallback method
  println(potentialFailure orElse backupMethod())
}

object PartialFunctions extends App {
  // Int => Int
  val aFunction = (x: Int) => x + 1

  // partial function
  // can be called like any other function
  // but throws a match error for any other value than the defined
  val aPartialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 56
    case 5 => 999
  }

  println(aPartialFunction.isDefinedAt(3))

  // lift to normal function
  // transforms a partial function to a full function returning Option[Int]
  val lifted = aPartialFunction.lift

  // chaining
  aPartialFunction.orElse[Int, Int] {
    case 42 => 42
  }

  // partial functions can be passed to normal function types
  val aTotalFunction: Int => Int = {
    case 42 => 42
  }

  // partial functions can be passed to HOCs like .map
  List(1, 2).map(aPartialFunction)
}
