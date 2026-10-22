package crap4s.output

import crap4s.core.Error
import crap4s.core.Threshold
import crap4s.core.UnitRow
import crap4s.pipeline.Report

import java.util.Locale

/** Aligned table for a terminal: all units, worst first. Rows above the threshold are red. */
final class HumanWriter(color: Boolean) extends Report:
  import HumanWriter.*

  def render(rows: List[UnitRow], threshold: Threshold, all: Boolean): Either[Error, String] =
    val sorted = UnitRow.select(rows, threshold, all = true)
    val cells = Header :: sorted.map(cellsOf)
    val widths = Table.widths(cells)
    val body = cells.zip(false :: sorted.map(_.crap.isAbove(threshold))).map: (row, over) =>
      paint(Table.format(row, widths), over)
    Right((body :+ summary(rows, threshold)).mkString("", "\n", "\n"))

  private def paint(line: String, over: Boolean): String =
    if color && over then s"$Red$line$Reset" else line
end HumanWriter

object HumanWriter:
  private val Red = "\u001b[31m"
  private val Reset = "\u001b[0m"
  private val Header = List("file:line", "unit", "cc", "cov", "crap", "")

  private def cellsOf(row: UnitRow): List[String] =
    List(
      s"${row.file}:${row.line.value}",
      row.unit.value,
      row.cc.value.toString,
      s"${Math.round(row.cov.value * 100)}%",
      number(row.crap.value),
      if row.noCov then "no-cov" else ""
    )

  private def summary(rows: List[UnitRow], threshold: Threshold): String =
    val over = rows.count(_.crap.isAbove(threshold))
    val max = number(rows.map(_.crap.value).maxOption.getOrElse(0.0))
    s"${rows.size} units, max CRAP $max, $over over threshold"

  private def number(value: Double): String =
    String.format(Locale.ROOT, "%.2f", Double.box(value))
end HumanWriter

/** Column widths and padding. The first two columns are left aligned, the others right aligned. */
private object Table:
  private val LeftColumns = 2

  def widths(rows: List[List[String]]): List[Int] =
    rows.transpose.map(_.map(_.length).max)

  def format(row: List[String], widths: List[Int]): String =
    row.zip(widths).zipWithIndex.map(pad).mkString("  ").stripTrailing

  private def pad(cell: ((String, Int), Int)): String =
    val ((text, width), index) = cell
    if index < LeftColumns then text.padTo(width, ' ') else text.reverse.padTo(width, ' ').reverse

/** Decides whether the output uses ANSI colour. */
object Colour:
  def enabled(env: String => Option[String], isTerminal: Boolean): Boolean =
    isTerminal && env("NO_COLOR").isEmpty
