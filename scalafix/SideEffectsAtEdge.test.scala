import scala.meta._

class SideEffectsAtEdgeTest extends munit.FunSuite {
  private val allow = List("crap4s/edge/", "crap4s/input/", "crap4s/Main.scala")

  private def messages(code: String, path: String = "src/crap4s/core/A.scala"): List[String] =
    SideEffectsAtEdge.findings(dialects.Scala3(code).parse[Source].get, path, allow).map(_.message)

  private def flagged(code: String, what: String): Unit =
    assertEquals(
      messages(code),
      List(s"SideEffectsAtEdge: $what is a side effect; keep it in the edge files")
    )

  test("flags sys.exit and System members") {
    flagged("def f = sys.exit(1)", "sys.exit")
    flagged("def f = System.exit(1)", "System.exit")
    flagged("def f = System.getenv(\"A\")", "System.getenv")
    flagged("def f = System.getProperty(\"a\")", "System.getProperty")
    flagged("def f = System.console()", "System.console")
    flagged("def f = java.lang.System.in", "System.in")
  }

  test("flags the System streams") {
    flagged("def f = System.out.println(a)", "System.out")
    flagged("def f = System.err", "System.err")
  }

  test("flags print and println") {
    flagged("def f = println(a)", "println")
    flagged("def f = print(a)", "print")
  }

  test("flags Console") {
    flagged("def f = Console", "Console")
  }

  test("allows a method that is called print on a value") {
    assertEquals(messages("def f = writer.print(a)"), Nil)
  }

  test("flags var") {
    flagged("object A { var x = 1 }", "var")
  }

  test("flags the java.io, Files and FileSystems imports") {
    flagged("import java.io.File", "java.io")
    flagged("import java.nio.file.Files", "java.nio.file.Files")
    flagged("import java.nio.file.{FileSystems, Path}", "java.nio.file.Files")
    flagged("import java.nio.file.*", "java.nio.file.Files")
  }

  test("allows the Path import") {
    assertEquals(messages("import java.nio.file.Path"), Nil)
  }

  test("flags a qualified name") {
    flagged("def f = java.io.File.pathSeparator", "java.io")
    flagged("def f = java.nio.file.Files.exists(p)", "java.nio.file.Files.exists")
  }

  test("flags the mutable collections") {
    flagged("import scala.collection.mutable", "scala.collection.mutable")
    flagged("import scala.collection.mutable.ListBuffer", "scala.collection.mutable")
    flagged("def f = mutable.Map()", "scala.collection.mutable")
    flagged("def f = scala.collection.mutable.Map()", "scala.collection.mutable")
  }

  test("allows the edge files") {
    val code = "import java.io.File\nobject A { var x = 1; def f = println(1) }"
    assertEquals(messages(code, "src/crap4s/edge/System.scala"), Nil)
    assertEquals(messages(code, "/w/src/crap4s/input/tasty/A.scala"), Nil)
    assertEquals(messages(code, "src/crap4s/Main.scala"), Nil)
  }

  test("allows test sources") {
    assertEquals(messages("var x = 1", "test/crap4s/ATest.scala"), Nil)
    assertEquals(messages("var x = 1", "src/A.test.scala"), Nil)
  }
}
