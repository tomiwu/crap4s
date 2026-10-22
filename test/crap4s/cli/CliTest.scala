package crap4s.cli

import crap4s.core.Error
import crap4s.core.ValidationError
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
    Cli.run(List("--tasty", dirs._1.toString, "--coverage", dirs._2.toString) ++ extra, base)

  test("a unit above the threshold gives code 1 and sorted agent lines"):
    val outcome = run("--threshold", "6")
    assertEquals(outcome.code, 1)
    val lines = outcome.out.linesIterator.toList
    assert(lines.last.startsWith("FAIL "), outcome.out)
    val scores = lines.init.map(_.split("crap=").last.toDouble)
    assert(scores.nonEmpty)
    assertEquals(scores, scores.sorted.reverse)

  test("no unit above the threshold gives code 0 and only the PASS line"):
    val outcome = run("--threshold", "100000")
    assertEquals(outcome.code, 0)
    assert(outcome.out.matches("PASS \\d+ max=\\d+\\.\\d\\d\n"), outcome.out)

  test("--all lists every unit"):
    val all = run("--threshold", "100000", "--all").out.linesIterator.toList
    assert(all.size > 2)
    assert(all.last.startsWith("PASS "))

  test("a bad option gives the error code and a message"):
    val outcome = Cli.run(List("--bogus"), base)
    assertEquals(outcome.code, 2)
    assert(outcome.err.contains("Unknown option: --bogus"))
    assertEquals(outcome.out, "")

  test("a validation error goes to stderr with code 2 and nothing on stdout"):
    val error = Error.InvalidValue(ValidationError.InvalidCoverage(2.0))
    assertEquals(
      Cli.failure(error),
      Outcome("", "Coverage must be from 0.0 to 1.0: 2.0\n", 2)
    )

  test("a missing directory gives code 2 and nothing on stdout"):
    val outcome = Cli.run(List("--tasty", "/no/such", "--coverage", "/no/such"), base)
    assertEquals(outcome.code, 2)
    assertEquals(outcome.out, "")
    assert(outcome.err.contains("/no/such"))

  test("--help prints the usage and gives code 0"):
    val outcome = Cli.run(List("--help"), base)
    assertEquals(outcome.code, 0)
    assert(outcome.out.startsWith("Usage: crap4s"))
    assertEquals(outcome.err, "")

  test("--help wins over other options"):
    assertEquals(Cli.run(List("--bogus", "--help"), base).code, 0)
end CliTest
