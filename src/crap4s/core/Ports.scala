package crap4s.core

/** A unit (method or lambda) with its place in a source file. `file` is a normalized path. */
final case class CodeUnit(
    file: String,
    startOffset: Int,
    endOffset: Int,
    line: SourceLine,
    name: UnitName,
    cc: Complexity
)

/** A statement of the coverage report. `file` is a normalized path. */
final case class Statement(file: String, startOffset: Int, endOffset: Int, executed: Boolean)

/** Input port: the units of the analyzed code. */
trait UnitSource:
  def units(): Either[Error, List[CodeUnit]]

/** Input port: the statements of the coverage report. */
trait CoverageSource:
  def statements(): Either[Error, List[Statement]]

/** Output port: renders the analyzed units as text. `all` lists every unit. */
trait ReportWriter:
  def render(rows: List[UnitRow], threshold: Threshold, all: Boolean): String
