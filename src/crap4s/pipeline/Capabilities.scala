package crap4s.pipeline

import crap4s.core.CodeUnit
import crap4s.core.Error
import crap4s.core.Statement
import crap4s.core.Threshold
import crap4s.core.UnitRow
import crap4s.core.UnitStats

/** Capability: the units of the analyzed code. */
opaque type Units = () => Either[Error, List[CodeUnit]]

object Units:
  def apply(f: () => Either[Error, List[CodeUnit]]): Units = f

  extension (u: Units) def run(): Either[Error, List[CodeUnit]] = u()

/** Capability: the statements of the coverage report. */
opaque type Coverage = () => Either[Error, List[Statement]]

object Coverage:
  def apply(f: () => Either[Error, List[Statement]]): Coverage = f

  extension (c: Coverage) def run(): Either[Error, List[Statement]] = c()

/** Capability: assigns the coverage statements to the units. */
opaque type Mapping = (List[CodeUnit], List[Statement]) => List[UnitStats]

object Mapping:
  def apply(f: (List[CodeUnit], List[Statement]) => List[UnitStats]): Mapping = f

  extension (m: Mapping)
    def run(units: List[CodeUnit], statements: List[Statement]): List[UnitStats] =
      m(units, statements)

/** Capability: computes the CRAP rows from the unit statistics. */
opaque type Scoring = List[UnitStats] => Either[Error, List[UnitRow]]

object Scoring:
  def apply(f: List[UnitStats] => Either[Error, List[UnitRow]]): Scoring = f

  extension (s: Scoring) def run(stats: List[UnitStats]): Either[Error, List[UnitRow]] = s(stats)

/** Capability: renders the analyzed units as text. `all` lists every unit. */
opaque type Report = (List[UnitRow], Threshold, Boolean) => Either[Error, String]

object Report:
  def apply(f: (List[UnitRow], Threshold, Boolean) => Either[Error, String]): Report = f

  extension (r: Report)
    def run(rows: List[UnitRow], threshold: Threshold, all: Boolean): Either[Error, String] =
      r(rows, threshold, all)

def units(using u: Units): Units = u
def coverage(using c: Coverage): Coverage = c
def mapping(using m: Mapping): Mapping = m
def scoring(using s: Scoring): Scoring = s
def report(using r: Report): Report = r
