package demo

import munit.FunSuite

class DemoTest extends FunSuite:
  test("sign"):
    assertEquals(Demo.sign(5), 1)
    assertEquals(Demo.sign(-5), -1)

  test("describe"):
    assertEquals(Demo.describe(0), "zero")
    assertEquals(Demo.describe(4), "even")

  test("bothPositive"):
    assert(Demo.bothPositive(1, 1))
    assert(!Demo.bothPositive(-1, 1))

  test("countDown"):
    assertEquals(Demo.countDown(3), 3)

  test("parse"):
    assertEquals(Demo.parse("7"), 7)

  test("doubled"):
    assertEquals(Demo.doubled(List(1, 2)), List(2, 4))

  test("point and inline"):
    assertEquals(Point(1, -2).norm1, 3)
    assertEquals(twice(2), 4)
