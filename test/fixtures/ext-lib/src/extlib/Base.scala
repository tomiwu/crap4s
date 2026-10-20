package extlib

class Base:
  def greet(name: String): String = if name.isEmpty then "nobody" else name

final case class Pair(left: Int, right: Int)

final class Flag(val on: Boolean):
  def &&(other: Flag): Flag = Flag(on && other.on)
