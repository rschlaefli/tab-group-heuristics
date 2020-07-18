package statistics

case class Age(
    lte1: Int = 0,
    lte2: Int = 0,
    lte5: Int = 0,
    lte10: Int = 0,
    lte30: Int = 0,
    lte60: Int = 0,
    gt60: Int = 0
) {

  def apply(num: Int): Age = num match {
    case num if num <= 1  => Age(lte1 = 1)
    case num if num <= 2  => Age(lte2 = 1)
    case num if num <= 5  => Age(lte5 = 1)
    case num if num <= 10 => Age(lte10 = 1)
    case num if num <= 30 => Age(lte30 = 1)
    case num if num <= 60 => Age(lte60 = 1)
    case _                => Age(gt60 = 1)
  }

  def +(other: Age) = Age(
    this.lte1 + other.lte1,
    this.lte2 + other.lte2,
    this.lte5 + other.lte5,
    this.lte10 + other.lte10,
    this.lte30 + other.lte30,
    this.lte60 + other.lte60,
    this.gt60 + other.gt60
  )

  def asCsv =
    Seq(
      this.lte1,
      this.lte2,
      this.lte5,
      this.lte10,
      this.lte30,
      this.lte60,
      this.gt60
    ).mkString(";")
}
