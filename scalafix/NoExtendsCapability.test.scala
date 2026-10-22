import scala.meta._

class NoExtendsCapabilityTest extends munit.FunSuite {
  private val names = Set("Units", "Report")

  private def messages(code: String): List[String] =
    NoExtendsCapability.findings(dialects.Scala3(code).parse[Source].get, names).map(_.message)

  private val flagged = "NoExtendsCapability: do not extend the capability Units; use it with ?=>"

  test("flags a class that extends a capability") {
    assertEquals(messages("class A extends Units"), List(flagged))
  }

  test("flags an object that extends a capability") {
    assertEquals(messages("object A extends Units"), List(flagged))
  }

  test("flags an enum case, a qualified name and an anonymous class") {
    assertEquals(messages("enum E { case A extends Units }").size, 1)
    assertEquals(messages("class A extends p.Units").size, 1)
    assertEquals(messages("val a = new Units {}").size, 1)
  }

  test("flags a capability among other parents") {
    assertEquals(messages("class A extends Base with Units").size, 1)
  }

  test("allows a type that is not a capability") {
    assertEquals(messages("class A extends Base"), Nil)
  }

  test("allows an opaque type with a bound and a use of the capability") {
    assertEquals(messages("opaque type Units <: () => Int = () => Int\ndef f(u: Units) = u"), Nil)
  }
}
