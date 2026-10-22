package crap4s.core

import munit.FunSuite

class EithersTest extends FunSuite:

  private def positive(n: Int): Either[String, Int] =
    Either.cond(n > 0, n * 2, s"bad $n")

  test("all Right gives the results in order"):
    assertEquals(traverse(List(1, 2, 3))(positive), Right(List(2, 4, 6)))

  test("the first Left is returned"):
    assertEquals(traverse(List(1, -2, -3))(positive), Left("bad -2"))

  test("an empty list gives an empty Right"):
    assertEquals(traverse(List.empty[Int])(positive), Right(Nil))
end EithersTest
