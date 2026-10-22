package crap4s.input.scoverage

import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class CoverageParserTest extends FunSuite:

  private def record(
      id: Int,
      path: String,
      method: String,
      branch: Boolean,
      desc: String = "x"
  ): String =
    List(
      id.toString,
      path,
      "p",
      "C",
      "Object",
      "p.C",
      method,
      "10",
      "20",
      "3",
      "sym",
      "Apply",
      branch.toString,
      "0",
      "false",
      desc,
      "\f"
    ).mkString("\n")

  private val header = "# Coverage data, format version: 3.0\n# comment\n"

  test("parses statements with all fields"):
    val Right(List(s)) =
      CoverageReportParser.parse(header + record(4, "src/A.scala", "f", true)): @unchecked
    assertEquals(s, CoverageStatement(4, "src/A.scala", 10, 20, 3, "f", true))

  test("accepts a multi-line description"):
    val text = header + record(0, "a.scala", "f", false, "one\ntwo") + "\n" +
      record(1, "a.scala", "g", false)
    assertEquals(CoverageReportParser.parse(text).map(_.map(_.id)), Right(List(0, 1)))

  test("normalizes paths against the base directory"):
    val base = Path.of("/proj")
    assertEquals(SourcePathNormalizer.normalize("/proj/src/A.scala", Some(base)), "src/A.scala")
    assertEquals(SourcePathNormalizer.normalize("./src/../src/A.scala", None), "src/A.scala")
    assertEquals(SourcePathNormalizer.normalize("src\\A.scala", Some(base)), "src/A.scala")
    assertEquals(SourcePathNormalizer.normalize("/other/A.scala", Some(base)), "/other/A.scala")

  test("damaged reports give errors"):
    assert(CoverageReportParser.parse("nonsense").isLeft)
    assert(CoverageReportParser.parse(header + "0\nshort\n\f\n").isLeft)
    val badNumber = record(1, "a", "f", false).replace("\n10\n", "\nten\n")
    assert(CoverageReportParser.parse(header + badNumber).isLeft)
    assert(CoverageReportParser.parse(header +
      record(1, "a", "f", false).replace("false\n0", "maybe\n0")).isLeft)

  test("measurements parse ids and reject damage"):
    assertEquals(MeasurementsParser.parse("m", "1\n2\n\n2\n"), Right(Set(1, 2)))
    assert(MeasurementsParser.parse("m", "1\nzz\n").isLeft)

  test("loader reports missing and damaged files"):
    val dir = Files.createTempDirectory("crap4s-cov-bad")
    assert(CoverageLoader.load(dir).isLeft)
    Files.writeString(dir.resolve("scoverage.coverage"), header)
    Files.writeString(dir.resolve("scoverage.measurements.1"), "oops\n")
    assert(CoverageLoader.load(dir).left.exists(_.message.contains("scoverage.measurements.1")))
    assert(CoverageLoader.load(dir.resolve("nope")).isLeft)

  test("loads a real report from the compiler"):
    val out = Files.createTempDirectory("crap4s-cov-out")
    val cov = Files.createTempDirectory("crap4s-cov")
    val code = Process(Seq(
      "scala-cli",
      "compile",
      "test/fixtures/tasty-sample",
      "-d",
      out.toString,
      "--scalac-option",
      s"-coverage-out:$cov",
      "-S",
      "3.10.0"
    )).!
    assertEquals(code, 0)
    Files.writeString(cov.resolve("scoverage.measurements.1"), "0\n1\n")
    CoverageLoader.load(cov, Some(Path.of(".").toAbsolutePath)) match
      case Left(message) => fail(message.message)
      case Right(data) =>
        assert(data.statements.nonEmpty)
        assert(data.statements.exists(_.methodName == "greet"))
        assert(data.statements.forall(_.sourcePath.endsWith("Sample.scala")))
        assertEquals(data.executedIds, Set(0, 1))
end CoverageParserTest
