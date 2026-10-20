package cc

object Cc:
  def plain(a: Int): Int = a + 1

  def oneIf(a: Int): Int =
    if a > 0 then 1 else 2

  def elseIf(a: Int): Int =
    if a > 0 then 1 else if a < 0 then 2 else 3

  def matchCases(a: Int): Int =
    a match
      case 1 => 1
      case 2 => 2
      case _ => 3

  def matchGuard(a: Int): Int =
    a match
      case x if x > 5 => 1
      case _ => 2

  def catchCases(f: () => Int): Int =
    try f()
    catch
      case _: IllegalStateException => 1
      case _: IllegalArgumentException => 2
      case _: RuntimeException => 3

  def catchGuard(f: () => Int): Int =
    try f()
    catch case e: RuntimeException if e.getMessage == null => 1

  def whileLoop(n: Int): Int =
    var i = 0
    while i < n do i += 1
    i

  def forWithoutGuard(xs: List[Int]): List[Int] =
    for x <- xs yield x + 1

  def forWithGuard(xs: List[Int]): List[Int] =
    for x <- xs if x > 0 yield x

  def onlyFinally(f: () => Int): Int =
    try f()
    finally println("done")

  def andOr(a: Boolean, b: Boolean, c: Boolean): Boolean =
    a && b || c

  def nestedLambda(xs: List[Int]): List[Int] =
    xs.map(x => if x > 0 then x else 0)

  def simpleLambda(xs: List[Int]): List[Int] =
    xs.map(_ + 1)

  def lambdaInLambda(xs: List[Int]): List[Int] =
    xs.map(x => if x > 0 then List(x).map(y => if y > 1 then y else 0).sum else 0)

  def localDefBranchy(a: Int): Int =
    def helper(b: Int): Int = if b > 0 then 1 else 2
    helper(a)

  def localDefSimple(a: Int): Int =
    def helper(b: Int): Int = b + 1
    helper(a)

  inline def inlineIf(a: Int): Int =
    if a > 0 then 1 else 2

  def callsInline(a: Int): Int = inlineIf(a)

  def andChain(a: Boolean, b: Boolean): Boolean = a && b

  def orChain(a: Boolean, b: Boolean, c: Boolean): Boolean = a || b || c

  def ifAnd(a: Boolean, b: Boolean): Int =
    if a && b then 1 else 2

  def userOperator(a: Flag, b: Flag): Flag = a && b

  final case class Flag(on: Boolean):
    def &&(other: Flag): Flag = Flag(on && other.on)
end Cc
