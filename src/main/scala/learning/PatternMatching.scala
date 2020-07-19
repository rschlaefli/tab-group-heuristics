package course

object PatternMatching extends App {
  case class Person(name: String, age: Int)
  val bob = Person("Bob", 19)

  val greeting = bob match {
    case Person(name, age) if age < 20 => s"Hello young $name!"
    case Person(name, age)             => s"Hello $name"
    case _                             => "Who are you?"
  }

  println(greeting)
}

object AllThePatterns extends App {
  val x: Any = "Scala"
  val constants = x match {
    case 1              => "a number"
    case "Scala"        => "SCALA"
    case true           => "a bool"
    case AllThePatterns => "singleton object"
    case _              => "hello"
  }

  val matchAnything = x match {
    case _ => "anything"
  }

  val matchAVariable = x match {
    case something => s"this is $something"
  }

  val matchATuple = x match {
    case (1, 2)      => "this is a given tuple"
    case (1, x)      => "this is another tuple"
    case (2, (1, 3)) => "nested tuples!"
  }

  val matchLists = x match {
    // case List(1, 2, 3) :+ 42     => "infix pattern"
    case List(1, _, _, _) => "list with 4 items"
    case List(1, _*)      => "list of arbitrary length"
    case 1 :: List(_)     => "infix pattern"
    // case list: List[Int]         => "matching type"
    case List(1, _) | List(2, _) => "two options"
  }

}
