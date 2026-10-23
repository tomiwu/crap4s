package crap4s

import munit.FunSuite

class GuardTest extends FunSuite:

  final private class Captured:
    val out = new StringBuilder
    val err = new StringBuilder
    val terminal: Terminal =
      Terminal(TerminalOps(() => true, out.append(_).discard, err.append(_).discard))

  extension (b: StringBuilder) private def discard: Unit = ()

  private def failingEnv(t: Throwable): Env =
    Env(EnvOps(_ => throw t, _ => None, () => "."))

  test("a throwing capability gives code 2 and one line without a stack trace"):
    val term = Captured()
    val code =
      App.main(List("--tasty", "x"))(using term.terminal, failingEnv(RuntimeException("boom")))
    assertEquals(code, 2)
    assertEquals(term.err.toString, "crap4s: internal error: java.lang.RuntimeException: boom\n")
    assertEquals(term.out.toString, "")

  test("a LinkageError gives the same one line"):
    val term = Captured()
    val code = App.main(Nil)(using term.terminal, failingEnv(NoSuchMethodError("isTerminal")))
    assertEquals(code, 2)
    assertEquals(
      term.err.toString,
      "crap4s: internal error: java.lang.NoSuchMethodError: isTerminal\n"
    )

  test("an error without a message still gives one line"):
    val term = Captured()
    assertEquals(guarded(throw IllegalStateException())(using term.terminal), 2)
    assertEquals(term.err.toString, "crap4s: internal error: java.lang.IllegalStateException: \n")

  test("a normal run writes to the terminal and gives its code"):
    val term = Captured()
    val env = Env(EnvOps(_ => None, _ => None, () => "."))
    assertEquals(App.main(List("--help"))(using term.terminal, env), 0)
    assert(term.out.nonEmpty)
end GuardTest
