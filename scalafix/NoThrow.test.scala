import scala.meta._

class NoThrowTest extends munit.FunSuite {
  private def messages(code: String, path: String): List[String] =
    NoThrow.findings(dialects.Scala3(code).parse[Source].get, path).map(_.message)

  private val flagged = "NoThrow: do not throw; return a Left(...) with a typed error"

  test("flags a throw in a source file") {
    assertEquals(messages("object A { def f = throw new Exception }", "src/crap4s/A.scala"), List(flagged))
  }

  test("flags a throw in an absolute path") {
    assertEquals(messages("def f = throw e", "/work/src/a/B.scala").size, 1)
  }

  test("allows a throw in test sources") {
    assertEquals(messages("def f = throw e", "test/crap4s/ATest.scala"), Nil)
    assertEquals(messages("def f = throw e", "src/A.test.scala"), Nil)
  }

  test("allows code without a throw") {
    assertEquals(messages("def f = Left(e)", "src/A.scala"), Nil)
  }
}
