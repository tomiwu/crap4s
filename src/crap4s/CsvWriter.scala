package crap4s

import java.util.Locale

object CsvWriter:
  val Header: String = "file,line,unit,cc,cov,crap"

  private val Special: Set[Char] = Set(',', '"', '\n', '\r')

  def render(rows: List[UnitRow]): String = (Header :: rows.map(line)).mkString("", "\n", "\n")

  def line(row: UnitRow): String =
    List(
      escape(row.file),
      row.line.toString,
      escape(row.unit),
      row.cc.toString,
      number(row.cov),
      number(row.crap)
    )
      .mkString(",")

  def escape(field: String): String =
    if field.exists(Special.contains) then
      "\"" + field.replace("\"", "\"\"") + "\""
    else field

  private def number(value: Double): String =
    String.format(Locale.ROOT, "%.2f", Double.box(value))
end CsvWriter
