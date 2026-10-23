package crap4s.cli

import crap4s.App
import crap4s.Outcome
import crap4s.core.Error
import crap4s.core.ValidationError
import crap4s.given
import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class CliTest extends FunSuite:

  private val base = Path.of("").toAbsolutePath

  private lazy val dirs: (Path, Path) =
    val fixture = Path.of("test/fixtures/cc-sample").toAbsolutePath.normalize
    val classes = Files.createTempDirectory("crap4s-cli-classes")
    val coverage = Files.createTempDirectory("crap4s-cli-coverage")
    val cmd =
      Seq("scala-cli", "compile", fixture.toString, "-d", classes.toString, "-S", "3.10.0") ++
        Seq("-O", s"-coverage-out:$coverage", "-O", s"-sourceroot:$fixture")
    assertEquals(Process(cmd).!, 0)
    (classes, coverage)

  private def run(extra: String*): Outcome =
    App.run(
      List("--tasty", dirs._1.toString, "--coverage", dirs._2.toString) ++ extra,
      base.toString
    )

  test("a unit above the threshold gives code 1 and sorted agent lines"):
    val outcome = run("--threshold", "6")
    assertEquals(outcome.code, ExitCode.OverThreshold)
    val lines = outcome.out.linesIterator.toList
    assert(lines.last.startsWith("FAIL "), outcome.out)
    val scores = lines.init.map(_.split("crap=").last.toDouble)
    assert(scores.nonEmpty)
    assertEquals(scores, scores.sorted.reverse)

  test("no unit above the threshold gives code 0 and only the PASS line"):
    val outcome = run("--threshold", "100000")
    assertEquals(outcome.code, ExitCode.Pass)
    assert(outcome.out.matches("PASS \\d+ max=\\d+\\.\\d\\d\n"), outcome.out)

  test("--all lists every unit"):
    val all = run("--threshold", "100000", "--all").out.linesIterator.toList
    assert(all.size > 2)
    assert(all.last.startsWith("PASS "))

  test("a bad option gives the error code and a message"):
    val outcome = App.run(List("--bogus"), base.toString)
    assertEquals(outcome.code, ExitCode.Error)
    assert(outcome.err.contains("Unknown option: --bogus"))
    assertEquals(outcome.out, "")

  test("a validation error goes to stderr with code 2 and nothing on stdout"):
    val error = Error.InvalidValue(ValidationError.InvalidCoverage(2.0))
    assertEquals(
      App.failure(error),
      Outcome("", "Coverage must be from 0.0 to 1.0: 2.0\n", ExitCode.Error)
    )

  test("a missing directory gives code 2 and nothing on stdout"):
    val outcome = App.run(List("--tasty", "/no/such", "--coverage", "/no/such"), base.toString)
    assertEquals(outcome.code, ExitCode.Error)
    assertEquals(outcome.out, "")
    assert(outcome.err.contains("/no/such"))

  test("--help prints the usage and gives code 0"):
    val outcome = App.run(List("--help"), base.toString)
    assertEquals(outcome.code, ExitCode.Pass)
    assert(outcome.out.startsWith("Usage: crap4s"))
    assertEquals(outcome.err, "")

  test("--help wins over other options"):
    assertEquals(App.run(List("--bogus", "--help"), base.toString).code, ExitCode.Pass)
end CliTest
