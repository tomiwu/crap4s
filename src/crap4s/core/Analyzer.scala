package crap4s.core

object Analyzer:
  def rows(units: UnitSource, coverage: CoverageSource): Either[Error, List[UnitRow]] =
    for
      found <- units.units()
      statements <- coverage.statements()
      rows <- toRows(CoverageMapper.map(found, statements))
    yield rows

  private def toRows(stats: List[UnitStats]): Either[Error, List[UnitRow]] =
    Eithers.traverse(stats)(toRow)

  private def toRow(stat: UnitStats): Either[Error, UnitRow] =
    Coverage
      .from(stat.cov)
      .map(cov =>
        UnitRow(stat.unit.file, stat.unit.line, stat.unit.name, stat.unit.cc, cov, stat.noCov)
      )
      .left
      .map(Error.InvalidValue(_))
end Analyzer
