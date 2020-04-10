package learning

object Implicits extends App {
  val pair = "Daniel" -> "555"
  val intPair = 1 -> 2

  case class Person(name: String) {
    def greet = s"Hello, I am $name"
  }

  implicit def fromStringToPerson(str: String): Person = Person(str)

  println("Peter".greet)
  // rewritten to println(fromStringToPerson("Peter").greet)
  // only works if there is exactly one matching implicit

  // implicit parameters
  def increment(x: Int)(implicit amount: Int) = x + amount
  implicit val defaultAmount = 10
  println(increment(2))

  // ordering
  implicit val reverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  println(List(1, 4, 5, 3, 2).sorted)

  case class Human(name: String, age: Int)

  val persons = List(
    Human("Steve", 30),
    Human("Amy", 22),
    Human("John", 66)
  )

  implicit val personOrdering: Ordering[Human] =
    Ordering.fromLessThan(_.age > _.age)

  println(persons.sorted)
}
