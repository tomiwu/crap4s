import scala.meta._

class PassThroughTest extends munit.FunSuite {
  private def messages(code: String): List[String] =
    PassThrough.findings(dialects.Scala3(code).parse[Source].get).map(_.message)

  private def flagged(code: String, expected: String): Unit =
    assertEquals(messages(code), List(expected))

  private def clean(code: String): Unit = assertEquals(messages(code), Nil)

  test("flags a call with the parameters of the def") {
    flagged(
      "object A { def load(a: Int, b: Int) = loader.load(a, b) }",
      "PassThrough: load only calls loader.load; inline it"
    )
  }

  test("flags a call with constants") {
    flagged(
      "object A { def run(a: Int) = exec(a, 1, \"x\") }",
      "PassThrough: run only calls exec; inline it"
    )
  }

  test("flags a call with a type ascription") {
    flagged(
      "object A { def run(a: Int): Int = (exec(a): Int) }",
      "PassThrough: run only calls exec; inline it"
    )
  }

  test("flags a call without arguments") {
    flagged("object A { def run() = exec() }", "PassThrough: run only calls exec; inline it")
  }

  test("flags a call in braces") {
    flagged(
      "object A { def run(a: Int) = { exec(a) } }",
      "PassThrough: run only calls exec; inline it"
    )
  }

  test("flags a call with a type argument and a second clause") {
    flagged(
      "object A { def run[T](a: T)(b: Int) = exec[T](a)(b) }",
      "PassThrough: run only calls exec; inline it"
    )
  }

  test("flags a call in a class in Scala 3 syntax") {
    flagged(
      "class A:\n  def run(a: Int) = exec(a)\n",
      "PassThrough: run only calls exec; inline it"
    )
  }

  test("does not flag an override") {
    clean("class A extends B { override def run(a: Int) = exec(a) }")
  }

  test("does not flag a trait implementation") {
    clean("class A extends B { def run(a: Int) = exec(a) }")
    clean("given G: B with { def run(a: Int) = exec(a) }")
  }

  test("does not flag the apply of a companion") {
    clean("case class A(a: Int)\nobject A { def apply(a: Int, b: Int) = build(a, b) }")
  }

  test("flags an apply that is not in a companion") {
    flagged(
      "object A { def apply(a: Int) = build(a) }",
      "PassThrough: apply only calls build; inline it"
    )
  }

  test("does not flag a def with the nowarn annotation") {
    clean("object A { @nowarn(\"crap4s:passthrough\") def run(a: Int) = exec(a) }")
  }

  test("flags a def with another nowarn annotation") {
    flagged(
      "object A { @nowarn(\"cat=deprecation\") def run(a: Int) = exec(a) }",
      "PassThrough: run only calls exec; inline it"
    )
  }

  test("does not flag a body with more than one call") {
    clean("object A { def run(a: Int) = { log(a); exec(a) } }")
    clean("object A { def run(a: Int) = exec(a).next(a) }")
  }

  test("does not flag an argument that is an expression") {
    clean("object A { def run(a: Int) = exec(a + 1) }")
    clean("object A { def run(a: Int) = exec(other(a)) }")
  }

  test("does not flag an argument that is not a parameter") {
    clean("object A { def run(a: Int) = exec(a, outer) }")
  }

  test("does not flag a body that is not a call") {
    clean("object A { def run(a: Int) = a }")
    clean("object A { def run(a: Int) = new B(a) }")
  }

  test("does not flag a recursive call") {
    clean("object A { def run(a: Int): Int = run(a) }")
  }

  test("does not flag a def without a body") {
    clean("trait A { def run(a: Int): Int }")
  }
}
