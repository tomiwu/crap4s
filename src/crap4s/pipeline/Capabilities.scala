package crap4s.pipeline

import crap4s.cli.ExitCode
import crap4s.core.CodeUnit
import crap4s.core.Error
import crap4s.core.Statement
import crap4s.core.Threshold
import crap4s.core.UnitRow
import crap4s.core.UnitStats
import crap4s.core.anyExceeds

/** The input of the mapping stage: the units and the coverage statements. */
final case class Inputs(units: List[CodeUnit], statements: List[Statement])

/** The output of the mapping stage: the statistics of each unit. */
final case class Mapped(stats: List[UnitStats])

/** The output of the scoring stage: the CRAP rows. */
final case class Scored(rows: List[UnitRow])

/** The text for stdout and the exit code of a run. */
final case class Result(out: String, code: ExitCode)

/** The result of a run: the exit code depends on the threshold. */
def resultOf(scored: Scored, threshold: Threshold, out: String): Result =
  Result(
    out,
    if anyExceeds(scored.rows, threshold) then ExitCode.OverThreshold else ExitCode.Pass
  )

/** Capability: the units of the analyzed code. */
opaque type Units <: () => Either[Error, List[CodeUnit]] = () => Either[Error, List[CodeUnit]]

object Units:
  def apply(f: () => Either[Error, List[CodeUnit]]): Units = f

/** Capability: the statements of the coverage report. */
opaque type Coverage <: () => Either[Error, List[Statement]] =
  () => Either[Error, List[Statement]]

object Coverage:
  def apply(f: () => Either[Error, List[Statement]]): Coverage = f

/** A pipeline step: turns an I into an O or fails. */
opaque type Stage[I, O] <: I => Either[Error, O] = I => Either[Error, O]

object Stage:
  def apply[I, O](f: I => Either[Error, O]): Stage[I, O] = f

/** Capability: renders the scored units as text and decides the exit code. */
type Report = Stage[Scored, Result]
