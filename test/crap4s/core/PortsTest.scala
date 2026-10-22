package crap4s.core

import munit.FunSuite

import crap4s.cli.Registry
import crap4s.output.AgentWriter

class PortsTest extends FunSuite:

  private def valid[A](result: Either[ValidationError, A]): A =
    result.fold(e => fail(e.message), identity)

  private val unit = CodeUnit(
    "src/A.scala",
    0,
    50,
    valid(SourceLine.from(3)),
    valid(UnitName.from("A.f")),
    valid(Complexity.from(2))
  )

  private class FakeUnits(result: Either[Error, List[CodeUnit]]) extends UnitSource:
    def units(): Either[Error, List[CodeUnit]] = result

  private class FakeCoverage(result: Either[Error, List[Statement]]) extends CoverageSource:
    def statements(): Either[Error, List[Statement]] = result

  private object FakeWriter extends ReportWriter:
    def render(rows: List[UnitRow], threshold: Threshold, all: Boolean): String =
      rows.map(_.unit.value).mkString(",") + "@" + threshold.value

  private val statements =
    List(Statement("src/A.scala", 5, 10, true), Statement("src/A.scala", 12, 20, false))

  test("the analyzer builds rows from fake sources"):
    val rows = Analyzer.rows(FakeUnits(Right(List(unit))), FakeCoverage(Right(statements)))
    val row = rows.toOption.get.head
    assertEquals(row.file, "src/A.scala")
    assertEquals(row.line.value, 3)
    assertEquals(row.unit.value, "A.f")
    assertEquals(row.cov.value, 0.5)
    assertEquals(row.crap.value, 2.5)

  test("a failing unit source fails the analyzer"):
    val error = Error.CannotRead("x", "gone")
    assertEquals(Analyzer.rows(FakeUnits(Left(error)), FakeCoverage(Right(Nil))), Left(error))

  test("a failing coverage source fails the analyzer"):
    val error = Error.CannotList("x", "gone")
    assertEquals(
      Analyzer.rows(FakeUnits(Right(List(unit))), FakeCoverage(Left(error))),
      Left(error)
    )

  test("a fake writer renders the rows and the threshold"):
    val rows = Analyzer.rows(FakeUnits(Right(List(unit))), FakeCoverage(Right(Nil))).toOption.get
    assertEquals(FakeWriter.render(rows, Threshold.Default, all = false), "A.f@6.0")

  test("the registry knows agent"):
    assertEquals(Registry.writer("agent"), Right(AgentWriter))

  test("the registry lists the known names for an unknown format"):
    val error = Registry.writer("xml").left.toOption.get
    assertEquals(error.message, "Unknown format: xml. Known formats: agent, human")

  test("the agent writer is the default format"):
    assertEquals(Registry.DefaultFormat, "agent")
end PortsTest
