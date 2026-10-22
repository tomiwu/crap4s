package crap4s.pipeline

import crap4s.cli.ExitCode
import crap4s.core.CodeUnit
import crap4s.core.Complexity
import crap4s.core.Error
import crap4s.core.SourceLine
import crap4s.core.Statement
import crap4s.core.Threshold
import crap4s.core.UnitName
import crap4s.core.UnitRow
import crap4s.core.ValidationError
import munit.FunSuite

class PipelineTest extends FunSuite:

  private def valid[A](result: Either[ValidationError, A]): A =
    result.fold(e => fail(e.message), identity)

  private def unit(cc: Int) = CodeUnit(
    "src/A.scala",
    0,
    50,
    valid(SourceLine.from(3)),
    valid(UnitName.from("A.f")),
    valid(Complexity.from(cc))
  )

  private class FakeUnits(result: Either[Error, List[CodeUnit]]) extends Units:
    def units(): Either[Error, List[CodeUnit]] = result

  private class FakeCoverage(result: Either[Error, List[Statement]]) extends Coverage:
    def statements(): Either[Error, List[Statement]] = result

  private class FakeReport(result: Either[Error, String] = Right("report")) extends Report:
    def render(rows: List[UnitRow], threshold: Threshold, all: Boolean): Either[Error, String] =
      result.map(_ + ":" + rows.map(_.unit.value).mkString(",") + "@" + threshold.value)

  private val covered = List(Statement("src/A.scala", 5, 10, true))

  private def run(
      units: Either[Error, List[CodeUnit]],
      statements: Either[Error, List[Statement]],
      report: Report = FakeReport()
  ): Either[Error, Result] =
    given Units = FakeUnits(units)
    given Coverage = FakeCoverage(statements)
    given Report = report
    pipeline(Threshold.Default, all = false)

  test("a covered simple unit passes"):
    assertEquals(
      run(Right(List(unit(1))), Right(covered)),
      Right(Result("report:A.f@6.0", ExitCode.Pass))
    )

  test("an uncovered complex unit is over the threshold"):
    assertEquals(run(Right(List(unit(5))), Right(Nil)).map(_.code), Right(ExitCode.OverThreshold))

  test("an error from the units fails the pipeline"):
    val error = Error.CannotRead("x", "gone")
    assertEquals(run(Left(error), Right(covered)), Left(error))

  test("an error from the coverage fails the pipeline"):
    val error = Error.CannotList("x", "gone")
    assertEquals(run(Right(List(unit(1))), Left(error)), Left(error))

  test("an error from the report fails the pipeline"):
    val error = Error.MissingOption("--x")
    assertEquals(run(Right(List(unit(1))), Right(covered), FakeReport(Left(error))), Left(error))
end PipelineTest
