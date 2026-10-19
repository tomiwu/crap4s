package sample

object Sample:
  def greet(name: java.lang.String): java.lang.String =
    name

class SampleClass:
  def add(a: Int, b: Int): Int =
    if a > 0 then a + b else b
