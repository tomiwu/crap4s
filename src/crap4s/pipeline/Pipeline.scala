package crap4s.pipeline

import crap4s.cli.ExitCode
import crap4s.core.CoverageMapper
import crap4s.core.Error
import crap4s.core.Threshold
import crap4s.core.UnitRow

/** The text for stdout and the exit code of a run. */
final case class Result(out: String, code: ExitCode)

/** The full flow: units, coverage statements, mapping, CRAP rows, report and exit code. */
def pipeline(
    threshold: Threshold,
    all: Boolean
): (Units, Coverage, Report) ?=> Either[Error, Result] =
  for
    found <- units.units()
    statements <- coverage.statements()
    rows <- UnitRow.fromStats(CoverageMapper.map(found, statements))
    text <- report.render(rows, threshold, all)
  yield Result(text, exitCode(rows, threshold))

private def exitCode(rows: List[UnitRow], threshold: Threshold): ExitCode =
  if UnitRow.exceeds(rows, threshold) then ExitCode.OverThreshold else ExitCode.Pass
