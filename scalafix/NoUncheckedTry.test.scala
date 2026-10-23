import scala.meta._

class NoUncheckedTryTest extends munit.FunSuite {
  private val edge = List("core/Eithers.scala", "Effects.scala")
  private val guards = List("Effects.scala")

  private def messages(code: String, path: String): List[String] =
    NoUncheckedTry.findings(dialects.Scala3(code).parse[Source].get, path, edge, guards).map(_.message)

  private val outside = "NoUncheckedTry: do not use try outside the edge files"
  private val notLeft = "NoUncheckedTry: a catch must return Left(...)"
  private val safe = "def f = try Right(1) catch case NonFatal(e) => Left(e)"

  test("flags a try outside the edge files") {
    assertEquals(messages(safe, "src/crap4s/A.scala"), List(outside))
  }

  test("flags a try in an absolute path") {
    assertEquals(messages(safe, "/work/src/crap4s/A.scala"), List(outside))
  }

  test("allows a try that returns Left in an edge file") {
    assertEquals(messages(safe, "src/crap4s/core/Eithers.scala"), Nil)
  }

  test("allows a Left with a type argument and a block") {
    val code = "def f = try Right(1) catch case e: X => { Left[E, Int](e) }"
    assertEquals(messages(code, "src/crap4s/core/Eithers.scala"), Nil)
  }

  test("flags a catch that does not return Left in an edge file") {
    val code = "def f = try 1 catch case e: X => 2"
    assertEquals(messages(code, "src/crap4s/core/Eithers.scala"), List(notLeft))
  }

  test("allows a catch that does not return Left in a guard file") {
    val code = "def f = try 1 catch case t: Throwable => 2"
    assertEquals(messages(code, "src/crap4s/Effects.scala"), Nil)
  }

  test("flags a catch with a handler") {
    assertEquals(messages("def f = try 1 catch handler", "src/crap4s/A.scala"), List(outside))
  }

  test("flags a try with a finally and no catch only outside the edge files") {
    assertEquals(messages("def f = try 1 finally g()", "src/crap4s/A.scala"), List(outside))
    assertEquals(messages("def f = try 1 finally g()", "src/crap4s/Effects.scala"), Nil)
  }

  test("allows a try in test sources") {
    assertEquals(messages(safe, "test/crap4s/ATest.scala"), Nil)
    assertEquals(messages(safe, "src/A.test.scala"), Nil)
  }
}
