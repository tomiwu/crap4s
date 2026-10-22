package crap4s

import java.util.Locale

/** Few-token output: one line for each reported unit, then one summary line. */
object AgentWriter extends ReportWriter:

  def render(rows: List[UnitRow], threshold: Threshold, all: Boolean): String =
    val lines = UnitRow.select(rows, threshold, all).map(line)
    (lines :+ summary(rows, threshold)).mkString("", "\n", "\n")

  def line(row: UnitRow): String =
    s"${row.file}:${row.line.value} ${row.unit.value} cc=${row.cc.value} " +
      s"cov=${number(row.cov.value)} crap=${number(row.crap.value)}"

  def summary(rows: List[UnitRow], threshold: Threshold): String =
    val failing = rows.count(_.crap.isAbove(threshold))
    val max = number(rows.map(_.crap.value).maxOption.getOrElse(0.0))
    if failing == 0 then s"PASS ${rows.size} max=$max" else s"FAIL $failing/${rows.size} max=$max"

  private def number(value: Double): String =
    String.format(Locale.ROOT, "%.2f", Double.box(value))
end AgentWriter
