package crap4s.core

final case class UnitRow(
    file: String,
    line: SourceLine,
    unit: UnitName,
    cc: Complexity,
    cov: Coverage,
    noCov: Boolean = false
):
  def crap: CrapScore = crapOf(cc, cov)

/** Keeps the rows above the threshold, or all rows. Sorts by CRAP, highest first. */
def selectRows(rows: List[UnitRow], threshold: Threshold, all: Boolean): List[UnitRow] =
  rows.filter(row => all || row.crap.isAbove(threshold)).sortBy(row => -row.crap.value)

def anyExceeds(rows: List[UnitRow], threshold: Threshold): Boolean =
  rows.exists(_.crap.isAbove(threshold))

def rowsFromStats(stats: List[UnitStats]): Either[Error, List[UnitRow]] =
  traverse(stats)(fromStat)

private def fromStat(stat: UnitStats): Either[Error, UnitRow] =
  Coverage
    .from(stat.cov)
    .map(cov =>
      UnitRow(stat.unit.file, stat.unit.line, stat.unit.name, stat.unit.cc, cov, stat.noCov)
    )
    .left
    .map(Error.InvalidValue(_))
