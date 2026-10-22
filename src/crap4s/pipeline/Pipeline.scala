package crap4s.pipeline

import crap4s.cli.ExitCode
import crap4s.core.Error
import crap4s.core.Threshold
import crap4s.core.UnitRow
import crap4s.core.anyExceeds

/** The text for stdout and the exit code of a run. */
final case class Result(out: String, code: ExitCode)

/** The full flow: units, coverage statements, mapping, CRAP rows, report and exit code. */
def pipeline(
    threshold: Threshold,
    all: Boolean
): (Units, Coverage, Mapping, Scoring, Report) ?=> Either[Error, Result] =
  for
    found <- units.run()
    statements <- coverage.run()
    rows <- scoring.run(mapping.run(found, statements))
    text <- report.run(rows, threshold, all)
  yield Result(text, exitCode(rows, threshold))

private def exitCode(rows: List[UnitRow], threshold: Threshold): ExitCode =
  if anyExceeds(rows, threshold) then ExitCode.OverThreshold else ExitCode.Pass
