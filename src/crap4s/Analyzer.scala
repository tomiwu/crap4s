package crap4s

object Analyzer:
  def rows(units: UnitSource, coverage: CoverageSource): Either[Error, List[UnitRow]] =
    for
      found <- units.units()
      statements <- coverage.statements()
      rows <- toRows(CoverageMapper.map(found, statements))
    yield rows

  private def toRows(stats: List[UnitStats]): Either[Error, List[UnitRow]] =
    stats.foldRight(Right(Nil): Either[Error, List[UnitRow]]): (stat, acc) =>
      for
        rest <- acc
        row <- toRow(stat)
      yield row :: rest

  private def toRow(stat: UnitStats): Either[Error, UnitRow] =
    Coverage
      .from(stat.cov)
      .map(cov =>
        UnitRow(stat.unit.file, stat.unit.line, stat.unit.name, stat.unit.cc, cov, stat.noCov)
      )
      .left
      .map(Error.InvalidValue(_))
end Analyzer
