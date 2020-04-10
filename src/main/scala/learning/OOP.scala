package learning

// a constructor param with val is accessible as a field (person1.age)
class Person(name: String, val age: Int) {
  // instance-level functionality

  def greet(name: String) = println(s"Hello, $name")
  def ++++++(name: String) = println(s"My awkward method was called $name")

  def apply(): String = s"Hi, I am apply for $name and $age"
}

// when extending classes with constructores, params need to be passed through
class Adult(name: String, age: Int, ssn: String) extends Person(name, age)

object Person {
  // "static" class-level functionality
  // class and object are companions
  val X = 2

  // often encompass factory methods inside the companion object
  def apply(mother: Person, father: Person): Person = {
    new Person("Bobby", 10)
  }
}

// objects can be defined similarly to classes
// but they do not receive any parameters
// also, objects are singleton instances by definition
object OOP extends App {
  val roland = new Person("Roland", 10)

  roland.greet("Vreni")

  // infix notation equivalent
  roland greet "Annabeth"

  // funky method names ("operators")
  roland ++++++ "Haha"
  println(1.+(3))

  // prefix notation (operators +, -, ~, !)
  println(-1 == 1.unary_-)

  // apply method
  println(roland())
}
