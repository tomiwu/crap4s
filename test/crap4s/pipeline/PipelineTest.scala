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
import crap4s.core.mapCoverage
import crap4s.core.rowsFromStats
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

  private val covered = List(Statement("src/A.scala", 5, 10, true))

  private val realMapping: Mapping = Stage.total(i => Mapped(mapCoverage(i.units, i.statements)))
  private val realScoring: Scoring = Stage(m => rowsFromStats(m.stats).map(Scored(_)))

  private def fakeReport(result: Either[Error, String] = Right("report")): Report =
    Stage: scored =>
      result.map: text =>
        val names = scored.rows.map(_.unit.value).mkString(",")
        resultOf(scored, Threshold.Default, text + ":" + names + "@" + Threshold.Default.value)

  private def run(
      units: Either[Error, List[CodeUnit]],
      statements: Either[Error, List[Statement]],
      report: Report = fakeReport(),
      mapping: Mapping = realMapping,
      scoring: Scoring = realScoring
  ): Either[Error, Result] =
    given Units = Units(() => units)
    given Coverage = Coverage(() => statements)
    given Mapping = mapping
    given Scoring = scoring
    given Report = report
    pipeline

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
    assertEquals(run(Right(List(unit(1))), Right(covered), fakeReport(Left(error))), Left(error))

  test("the mapping gets the units and the statements"):
    val seen = List.newBuilder[(Int, Int)]
    val spy: Mapping = Stage: inputs =>
      seen += ((inputs.units.size, inputs.statements.size))
      Right(Mapped(Nil))
    val _ = run(Right(List(unit(1))), Right(covered), mapping = spy)
    assertEquals(seen.result(), List((1, 1)))

  test("an error from the scoring fails the pipeline"):
    val error = Error.MissingOption("--score")
    val failing: Scoring = Stage(_ => Left(error))
    assertEquals(run(Right(List(unit(1))), Right(covered), scoring = failing), Left(error))

  test("a fake scoring decides the rows and the exit code"):
    val row = UnitRow(
      "src/A.scala",
      valid(SourceLine.from(3)),
      valid(UnitName.from("A.fake")),
      valid(Complexity.from(30)),
      valid(crap4s.core.Coverage.from(0.0))
    )
    val result = run(Right(Nil), Right(Nil), scoring = Stage(_ => Right(Scored(List(row)))))
    assertEquals(result, Right(Result("report:A.fake@6.0", ExitCode.OverThreshold)))
end PipelineTest
