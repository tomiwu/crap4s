import scala.meta._

class NoUnsafeCallTest extends munit.FunSuite {
  private def messages(code: String, path: String = "src/crap4s/A.scala"): List[String] =
    NoUnsafeCall.findings(dialects.Scala3(code).parse[Source].get, path).map(_.message)

  private def flagged(code: String, name: String): Unit =
    assertEquals(
      messages(code),
      List(s"NoUnsafeCall: $name can throw; return a value in an Either or an Option")
    )

  test("flags get without arguments") {
    flagged("def f = o.get", "get")
  }

  test("allows get with an argument") {
    assertEquals(messages("def f = m.get(k)"), Nil)
    assertEquals(messages("def f = m.get[X](k)"), Nil)
  }

  test("flags head, last, tail, reduce, toInt and toDouble") {
    flagged("def f = xs.head", "head")
    flagged("def f = xs.last", "last")
    flagged("def f = xs.tail", "tail")
    flagged("def f = xs.reduce(_ + _)", "reduce")
    flagged("def f = s.toInt", "toInt")
    flagged("def f = s.toDouble", "toDouble")
  }

  test("allows the safe forms") {
    assertEquals(messages("def f = xs.headOption.lastOption.toIntOption"), Nil)
  }

  test("flags require and assert") {
    flagged("def f = require(a > 0)", "require")
    flagged("def f = assert(a > 0, \"x\")", "assert")
  }

  test("allows a def that has a similar name") {
    assertEquals(messages("def f = requireDirectories(a)"), Nil)
  }

  test("flags ???") {
    flagged("def f: Int = ???", "???")
  }

  test("allows test sources") {
    assertEquals(messages("def f = o.get", "test/crap4s/ATest.scala"), Nil)
    assertEquals(messages("def f = o.get", "src/A.test.scala"), Nil)
  }
}
