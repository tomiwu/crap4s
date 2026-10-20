package crap4s

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

  test("a unit above the threshold gives a code that is not 0 and a sorted CSV"):
    val outcome = run("--threshold", "6")
    assertEquals(outcome.code, Cli.AboveThresholdCode)
    val rows = outcome.out.linesIterator.toList
    assertEquals(rows.head, CsvWriter.Header)
    assert(rows.size > 1)
    val scores = rows.tail.map(_.split(',').last.toDouble)
    assertEquals(scores, scores.sorted.reverse)

  test("no unit above the threshold gives code 0 and only the header"):
    val outcome = run("--threshold", "100000")
    assertEquals(outcome.code, 0)
    assertEquals(outcome.out, CsvWriter.Header + "\n")

  test("--all lists every unit"):
    val all = run("--threshold", "100000", "--all").out.linesIterator.toList
    assert(all.size > 2)

  test("a bad option gives the error code and a message"):
    val outcome = Cli.run(List("--bogus"), base)
    assertEquals(outcome.code, Cli.ErrorCode)
    assert(outcome.err.contains("Unknown option: --bogus"))
    assertEquals(outcome.out, "")
end CliTest
