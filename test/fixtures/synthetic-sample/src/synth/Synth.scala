package synth

final case class Point(x: Int, y: Int):
  def norm: Int = x + y

enum Color derives CanEqual:
  case Red, Green

  def isRed: Boolean = this == Red

object Synth:
  inline def twice(a: Int): Int =
    if a > 0 then a * 2 else a

  def callsTwice(a: Int): Int = twice(a)
