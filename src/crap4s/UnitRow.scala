package crap4s

final case class UnitRow(
    file: String,
    line: SourceLine,
    unit: UnitName,
    cc: Complexity,
    cov: Coverage,
    noCov: Boolean = false
):
  def crap: CrapScore = Crap.of(cc, cov)

object UnitRow:
  /** Keeps the rows above the threshold, or all rows. Sorts by CRAP, highest first. */
  def select(rows: List[UnitRow], threshold: Threshold, all: Boolean): List[UnitRow] =
    rows.filter(row => all || row.crap.isAbove(threshold)).sortBy(row => -row.crap.value)

  def exceeds(rows: List[UnitRow], threshold: Threshold): Boolean =
    rows.exists(_.crap.isAbove(threshold))
