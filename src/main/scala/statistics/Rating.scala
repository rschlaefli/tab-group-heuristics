package statistics

case class Rating(
    is1: Int = 0,
    is2: Int = 0,
    is3: Int = 0,
    is4: Int = 0,
    is5: Int = 0
) {

  def +(other: Rating) = Rating(
    this.is1 + other.is1,
    this.is2 + other.is2,
    this.is3 + other.is3,
    this.is4 + other.is4,
    this.is5 + other.is5
  )

  def asCsv =
    Seq(this.is1, this.is2, this.is3, this.is4, this.is5).mkString(";")
}

object Rating {
  def apply(num: Int): Rating = num match {
    case num if num == 1 => Rating(is1 = 1)
    case num if num == 2 => Rating(is2 = 1)
    case num if num == 3 => Rating(is3 = 1)
    case num if num == 4 => Rating(is4 = 1)
    case num if num == 5 => Rating(is5 = 1)
    case _               => Rating()
  }
}
