package crap4s

final case class UnitRow(file: String, line: Int, unit: String, cc: Int, cov: Double):
  def crap: Double = CrapScore.of(cc, cov)

object UnitRow:
  /** Keeps the rows above the threshold, or all rows. Sorts by CRAP, highest first. */
  def select(rows: List[UnitRow], threshold: Double, all: Boolean): List[UnitRow] =
    rows.filter(row => all || row.crap > threshold).sortBy(row => -row.crap)

  def exceeds(rows: List[UnitRow], threshold: Double): Boolean =
    rows.exists(_.crap > threshold)
