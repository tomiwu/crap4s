package demo

case class Point(x: Int, y: Int):
  def norm1: Int = x.abs + y.abs

inline def twice(inline n: Int): Int = n + n

object Demo:
  def sign(n: Int): Int =
    if n > 0 then 1
    else if n < 0 then -1
    else 0

  def describe(n: Int): String = n match
    case 0 => "zero"
    case x if x % 2 == 0 => "even"
    case _ => "odd"

  def bothPositive(a: Int, b: Int): Boolean = a > 0 && b > 0

  def countDown(start: Int): Int =
    var n = start
    var steps = 0
    while n > 0 do
      n -= 1
      steps += 1
    steps

  def parse(text: String): Int =
    try text.toInt
    catch case _: NumberFormatException => -1

  def doubled(xs: List[Int]): List[Int] = xs.map(x => x * 2)

  def untested(n: Int): Int =
    if n > 10 && n < 20 then n
    else if n > 100 then n - 100
    else 0
end Demo
