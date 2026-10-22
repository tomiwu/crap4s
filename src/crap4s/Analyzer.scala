package crap4s

import crap4s.coverage.CoverageData
import crap4s.coverage.CoverageMapper
import crap4s.coverage.SourcePathNormalizer
import crap4s.coverage.UnitCoverage
import crap4s.tasty.CyclomaticComplexity
import crap4s.tasty.TastyLoadResult

import java.nio.file.Path

object Analyzer:
  def rows(
      tasty: TastyLoadResult,
      data: CoverageData,
      baseDir: Path
  ): Either[Error, List[UnitRow]] =
    given tastyquery.Contexts.Context = tasty.context
    val units = CoverageMapper.map(tasty.methods, data, Some(baseDir))
    units.foldRight(Right(Nil): Either[Error, List[UnitRow]]): (unit, acc) =>
      for
        rest <- acc
        row <- toRow(unit, baseDir)
      yield row :: rest

  private def toRow(uc: UnitCoverage, baseDir: Path)(using
      tastyquery.Contexts.Context
  ): Either[Error, UnitRow] =
    val unit = uc.unit
    val row =
      for
        line <- SourceLine.from(unit.position.line)
        name <- UnitName.from(s"${unit.owner}.${unit.name}")
        cc <- Complexity.from(CyclomaticComplexity.ofUnit(unit))
        cov <- Coverage.from(uc.cov)
      yield UnitRow(
        file = SourcePathNormalizer.normalize(unit.position.sourceFile, Some(baseDir)),
        line = line,
        unit = name,
        cc = cc,
        cov = cov
      )
    row.left.map(Error.InvalidValue(_))
end Analyzer
