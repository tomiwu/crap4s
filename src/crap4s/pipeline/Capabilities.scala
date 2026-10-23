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

/** Loads the units and the coverage statements. */
def loadInputs(using u: Units, c: Coverage): Either[Error, Inputs] =
  for
    found <- u()
    statements <- c()
  yield Inputs(found, statements)

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

/** A pipeline step from A to B that can fail. Make it with `Stage.total` or `Stage(...)`. */
opaque type Stage[A, B] <: (A => Either[Error, B]) = A => Either[Error, B]

object Stage:
  def apply[A, B](f: A => Either[Error, B]): Stage[A, B] = f
  def total[A, B](f: A => B): Stage[A, B] = f.andThen(Right(_))

/** Capability: assigns the coverage statements to the units. */
type Mapping = Stage[Inputs, Mapped]

/** Capability: computes the CRAP rows from the unit statistics. */
type Scoring = Stage[Mapped, Scored]

/** Capability: renders the scored units as text and decides the exit code. */
type Report = Stage[Scored, Result]

def units(using u: Units): Units = u
def coverage(using c: Coverage): Coverage = c
def mapping(using m: Mapping): Mapping = m
def scoring(using s: Scoring): Scoring = s
def report(using r: Report): Report = r
