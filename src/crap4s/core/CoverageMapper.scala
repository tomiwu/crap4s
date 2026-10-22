package crap4s.core

final case class UnitStats(unit: CodeUnit, total: Int, executed: Int):
  /** Executed share of the statements. A unit without statements has `NoStatementsCoverage`. */
  def cov: Double =
    if noCov then CoverageMapper.NoStatementsCoverage else executed.toDouble / total

  /** True when the report has no statements for the unit. The output marks it `no-cov`. */
  def noCov: Boolean = total == 0

object CoverageMapper:
  /**
   * Default for a unit that has no statements in the report (PRD decision: coverage 0.0, so CRAP =
   * cc² + cc).
   */
  val NoStatementsCoverage: Double = 0.0

  /** Assigns each statement to the narrowest unit whose range `[start, end)` contains it. */
  def map(units: List[CodeUnit], statements: List[Statement]): List[UnitStats] =
    val indexed = units.zipWithIndex
    val owned = statements.flatMap(s => owner(indexed, s).map(_ -> s)).groupMap(_._1)(_._2)
    indexed.map: (unit, index) =>
      val own = owned.getOrElse(index, Nil)
      UnitStats(unit, own.size, own.count(_.executed))

  private def length(unit: CodeUnit): Int = unit.endOffset - unit.startOffset

  private def contains(unit: CodeUnit, s: Statement): Boolean =
    unit.file == s.file && unit.startOffset <= s.startOffset && s.endOffset <= unit.endOffset

  private def owner(indexed: List[(CodeUnit, Int)], s: Statement): Option[Int] =
    indexed.filter((unit, _) => contains(unit, s)).minByOption((unit, _) => length(unit)).map(_._2)
end CoverageMapper
