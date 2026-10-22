package crap4s.pipeline

import crap4s.core.CodeUnit
import crap4s.core.Error
import crap4s.core.Statement
import crap4s.core.Threshold
import crap4s.core.UnitRow

/** Capability: the units of the analyzed code. */
trait Units:
  def units(): Either[Error, List[CodeUnit]]

/** Capability: the statements of the coverage report. */
trait Coverage:
  def statements(): Either[Error, List[Statement]]

/** Capability: renders the analyzed units as text. `all` lists every unit. */
trait Report:
  def render(rows: List[UnitRow], threshold: Threshold, all: Boolean): Either[Error, String]

def units(using u: Units): Units = u
def coverage(using c: Coverage): Coverage = c
def report(using r: Report): Report = r
