package crap4s

import crap4s.coverage.CoverageData
import crap4s.coverage.CoverageMapper
import crap4s.coverage.SourcePathNormalizer
import crap4s.coverage.UnitCoverage
import crap4s.tasty.CyclomaticComplexity
import crap4s.tasty.TastyLoadResult

import java.nio.file.Path

object Analyzer:
  def rows(tasty: TastyLoadResult, data: CoverageData, baseDir: Path): List[UnitRow] =
    given tastyquery.Contexts.Context = tasty.context
    CoverageMapper.map(tasty.methods, data, Some(baseDir)).map(toRow(_, baseDir))

  private def toRow(uc: UnitCoverage, baseDir: Path)(using tastyquery.Contexts.Context): UnitRow =
    val unit = uc.unit
    UnitRow(
      file = SourcePathNormalizer.normalize(unit.position.sourceFile, Some(baseDir)),
      line = unit.position.line,
      unit = s"${unit.owner}.${unit.name}",
      cc = CyclomaticComplexity.ofUnit(unit),
      cov = uc.cov
    )
