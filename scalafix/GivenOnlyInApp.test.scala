import scala.meta._

class GivenOnlyInAppTest extends munit.FunSuite {
  private val names = Set("Units")

  private def messages(code: String, path: String): List[String] =
    GivenOnlyInApp.findings(dialects.Scala3(code).parse[Source].get, names, path).map(_.message)

  private val flagged = "GivenOnlyInApp: give the capability Units only in App.scala"
  private val alias = "object A { given Units = u }"

  test("flags a given alias in a source file") {
    assertEquals(messages(alias, "src/crap4s/Other.scala"), List(flagged))
  }

  test("flags a named given and a given with a body") {
    assertEquals(messages("object A { given u: Units = x }", "src/Other.scala").size, 1)
    assertEquals(messages("object A { given Units with { def apply() = 1 } }", "src/O.scala").size, 1)
  }

  test("allows a given in App.scala") {
    assertEquals(messages(alias, "src/crap4s/App.scala"), Nil)
  }

  test("allows a given in test sources") {
    assertEquals(messages(alias, "test/crap4s/PipelineTest.scala"), Nil)
    assertEquals(messages(alias, "/work/test/a/B.scala"), Nil)
    assertEquals(messages(alias, "src/Pipeline.test.scala"), Nil)
  }

  test("allows a given that is not a capability") {
    assertEquals(messages("object A { given Context = c }", "src/Other.scala"), Nil)
  }
}
