package crap4s.output

import crap4s.core.Error
import crap4s.core.Threshold
import crap4s.core.UnitRow
import crap4s.core.selectRows
import crap4s.pipeline.Report
import crap4s.pipeline.Result

import java.util.Locale

/** Aligned table for a terminal: all units, worst first. Rows above the threshold are red. */
def humanReport(color: Boolean, threshold: Threshold): Report =
  Report: scored =>
    renderHuman(color, scored.rows, threshold).map(Result.of(scored, threshold, _))

def renderHuman(color: Boolean, rows: List[UnitRow], threshold: Threshold): Either[Error, String] =
  val sorted = selectRows(rows, threshold, all = true)
  val cells = Header :: sorted.map(cellsOf)
  val widths = tableWidths(cells)
  val body = cells.zip(false :: sorted.map(_.crap.isAbove(threshold))).map: (row, over) =>
    paint(color, formatRow(row, widths), over)
  Right((body :+ humanSummary(rows, threshold)).mkString("", "\n", "\n"))

private def paint(color: Boolean, line: String, over: Boolean): String =
  if color && over then s"$Red$line$Reset" else line

private val Red = "\u001b[31m"
private val Reset = "\u001b[0m"
private val Header = List("file:line", "unit", "cc", "cov", "crap", "")

private def cellsOf(row: UnitRow): List[String] =
  List(
    s"${row.file}:${row.line.value}",
    row.unit.value,
    row.cc.value.toString,
    s"${Math.round(row.cov.value * 100)}%",
    humanNumber(row.crap.value),
    if row.noCov then "no-cov" else ""
  )

private def humanSummary(rows: List[UnitRow], threshold: Threshold): String =
  val over = rows.count(_.crap.isAbove(threshold))
  val max = humanNumber(rows.map(_.crap.value).maxOption.getOrElse(0.0))
  s"${rows.size} units, max CRAP $max, $over over threshold"

private def humanNumber(value: Double): String =
  String.format(Locale.ROOT, "%.2f", Double.box(value))

/** Column widths and padding. The first two columns are left aligned, the others right aligned. */
private val LeftColumns = 2

private def tableWidths(rows: List[List[String]]): List[Int] =
  rows.transpose.map(_.map(_.length).max)

private def formatRow(row: List[String], widths: List[Int]): String =
  row.zip(widths).zipWithIndex.map(pad).mkString("  ").stripTrailing

private def pad(cell: ((String, Int), Int)): String =
  val ((text, width), index) = cell
  if index < LeftColumns then text.padTo(width, ' ') else text.reverse.padTo(width, ' ').reverse

/** Decides whether the output uses ANSI colour. */
def colourEnabled(env: String => Option[String], isTerminal: Boolean): Boolean =
  isTerminal && env("NO_COLOR").isEmpty
