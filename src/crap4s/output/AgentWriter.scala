package crap4s.output

import crap4s.core.Error
import crap4s.core.Threshold
import crap4s.core.UnitRow
import crap4s.core.selectRows
import crap4s.pipeline.Report
import crap4s.pipeline.resultOf

import java.util.Locale

/** Few-token output: one line for each reported unit, then one summary line. */
def agentReport(threshold: Threshold, all: Boolean): Report =
  Report: scored =>
    renderAgent(scored.rows, threshold, all).map(resultOf(scored, threshold, _))

def renderAgent(rows: List[UnitRow], threshold: Threshold, all: Boolean): Either[Error, String] =
  val lines = selectRows(rows, threshold, all).map(agentLine)
  Right((lines :+ agentSummary(rows, threshold)).mkString("", "\n", "\n"))

def agentLine(row: UnitRow): String =
  s"${row.file}:${row.line.value} ${row.unit.value} cc=${row.cc.value} " +
    s"cov=${agentNumber(row.cov.value)} crap=${agentNumber(row.crap.value)}" +
    (if row.noCov then " no-cov" else "")

def agentSummary(rows: List[UnitRow], threshold: Threshold): String =
  val failing = rows.count(_.crap.isAbove(threshold))
  val max = agentNumber(rows.map(_.crap.value).maxOption.getOrElse(0.0))
  if failing == 0 then s"PASS ${rows.size} max=$max" else s"FAIL $failing/${rows.size} max=$max"

private def agentNumber(value: Double): String =
  String.format(Locale.ROOT, "%.2f", Double.box(value))
