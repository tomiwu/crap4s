package crap4s.coverage

import crap4s.tasty.LoadedMethod

import java.nio.file.Path

final case class UnitCoverage(unit: LoadedMethod, total: Int, executed: Int):
  /** Executed share of the statements. A unit without statements has `NoStatementsCoverage`. */
  def cov: Double =
    if total == 0 then CoverageMapper.NoStatementsCoverage else executed.toDouble / total

object CoverageMapper:
  /** Default for a unit that has no statements in the report (PRD decision is TBD). */
  val NoStatementsCoverage: Double = 0.0

  /** Assigns each statement to the narrowest unit whose range `[start, end)` contains it. */
  def map(
      units: List[LoadedMethod],
      data: CoverageData,
      baseDir: Option[Path]
  ): List[UnitCoverage] =
    val indexed = units.zipWithIndex.map((unit, i) => IndexedUnit(i, key(unit, baseDir), unit))
    val owned =
      data.statements.flatMap(s => owner(indexed, s, baseDir).map(_ -> s)).groupMap(_._1)(_._2)
    indexed.map: entry =>
      val own = owned.getOrElse(entry.index, Nil)
      UnitCoverage(entry.unit, own.size, own.count(s => data.executedIds.contains(s.id)))

  final private case class IndexedUnit(index: Int, file: String, unit: LoadedMethod)

  private def key(unit: LoadedMethod, baseDir: Option[Path]): String =
    SourcePathNormalizer.normalize(unit.position.sourceFile, baseDir)

  private def length(unit: LoadedMethod): Int =
    unit.position.endOffset - unit.position.startOffset

  private def contains(unit: LoadedMethod, s: CoverageStatement): Boolean =
    unit.position.startOffset <= s.startOffset && s.endOffset <= unit.position.endOffset

  private def owner(
      indexed: List[IndexedUnit],
      s: CoverageStatement,
      baseDir: Option[Path]
  ): Option[Int] =
    val path = SourcePathNormalizer.normalize(s.sourcePath, baseDir)
    indexed
      .filter(e => e.file == path && contains(e.unit, s))
      .minByOption(e => length(e.unit))
      .map(_.index)
end CoverageMapper
