package crap4s.cli

import crap4s.App
import crap4s.Env
import crap4s.EnvOps
import crap4s.Terminal
import crap4s.TerminalOps
import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class BadInputsTest extends FunSuite:

  private val fixture = Path.of("test/fixtures/cc-sample").toAbsolutePath.normalize

  private lazy val good: (Path, Path) =
    val classes = Files.createTempDirectory("crap4s-bad-classes")
    val coverage = Files.createTempDirectory("crap4s-bad-coverage")
    val cmd =
      Seq("scala-cli", "compile", fixture.toString, "-d", classes.toString, "-S", "3.10.0") ++
        Seq("-O", s"-coverage-out:$coverage", "-O", s"-sourceroot:$fixture")
    assertEquals(Process(cmd).!, 0)
    (classes, coverage)

  private def tasty: String = good._1.toString

  private def coverageWith(file: String, content: String): String =
    val dir = Files.createTempDirectory("crap4s-bad-cov")
    Files.list(good._2).forEach(f => Files.copy(f, dir.resolve(f.getFileName)): Unit)
    Files.writeString(dir.resolve(file), content)
    dir.toString

  private def tempFile(name: String, content: String): String =
    val file = Files.createTempDirectory("crap4s-bad-file").resolve(name)
    Files.writeString(file, content)
    file.toString

  private def cli(args: List[String]): (Int, String, String) =
    val out = new StringBuilder
    val err = new StringBuilder
    val terminal = Terminal(
      TerminalOps(
        () => false,
        text => out.append(text).discard,
        text => err.append(text).discard
      )
    )
    val env = Env(EnvOps(_ => None, _ => None, () => Path.of("").toAbsolutePath.toString))
    val code = App.main(args)(using terminal, env)
    (code, out.toString, err.toString)

  extension (b: StringBuilder) private def discard: Unit = ()

  private def check(name: String)(args: => List[String]): Unit =
    test(name):
      val (code, out, err) = cli(args)
      assertEquals(code, 2, err)
      assertEquals(out, "")
      assertEquals(err.linesIterator.size, 1, err)
      assert(err.startsWith("crap4s:"), err)
      List("Exception", "\tat ", "at crap4s.").foreach(bad => assert(!err.contains(bad), err))

  private def withGood(extra: String*): List[String] =
    List("--tasty", tasty, "--coverage", good._2.toString) ++ extra

  check("a missing tasty directory")(List("--tasty", "/no/such", "--coverage", good._2.toString))
  check("a missing coverage directory")(List("--tasty", tasty, "--coverage", "/no/such"))
  check("a broken scoverage.coverage file"):
    List("--tasty", tasty, "--coverage", coverageWith("scoverage.coverage", "garbage\n"))
  check("a broken measurements file"):
    List(
      "--tasty",
      tasty,
      "--coverage",
      coverageWith("scoverage.measurements.1", "not-a-number\n")
    )
  check("a corrupt jar in the classpath"):
    withGood("--classpath", tempFile("corrupt.jar", "this is not a jar"))
  check("a classpath file that does not exist")(withGood("--classpath-file", "/no/such/cp.txt"))
  check("an invalid path string")(withGood("--classpath-file", "bad\u0000path"))
end BadInputsTest
