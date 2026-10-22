import scala.meta._

class CapabilityObjectTest extends munit.FunSuite {
  private def messages(code: String): List[String] =
    CapabilityObject.findings(dialects.Scala3(code).parse[Source].get).map(_.message)

  test("flags an object with only defs") {
    assertEquals(
      messages("object Helpers { def a = 1; def b(x: Int) = x }"),
      List("CapabilityObject: Helpers holds only functions; use top-level defs")
    )
  }

  test("flags an object in a package") {
    assertEquals(messages("package p\nobject Helpers { def a = 1 }").size, 1)
  }

  test("allows the companion of an opaque type") {
    val code = "opaque type Units = Int\nobject Units { def apply(i: Int): Units = i }"
    assertEquals(messages(code), Nil)
  }

  test("flags the companion of a type that is not opaque") {
    assertEquals(messages("type Units = Int\nobject Units { def apply(i: Int): Units = i }").size, 1)
  }

  test("allows the object App") {
    assertEquals(messages("object App { def run = 1 }"), Nil)
  }

  test("allows an object with a val") {
    assertEquals(messages("object A { val x = 1; def a = x }"), Nil)
  }

  test("allows an empty object") {
    assertEquals(messages("object A"), Nil)
  }
}
