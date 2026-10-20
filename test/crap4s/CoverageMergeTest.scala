package crap4s

import crap4s.coverage.CoverageData
import crap4s.coverage.CoverageStatement
import munit.FunSuite

class CoverageMergeTest extends FunSuite:

  private def statement(id: Int, path: String): CoverageStatement =
    CoverageStatement(id, path, 0, 1, 1, "m", false)

  test("merge keeps statement ids unique and shifts the executed ids"):
    val first = CoverageData(List(statement(0, "A.scala"), statement(1, "A.scala")), Set(1))
    val second = CoverageData(List(statement(0, "B.scala")), Set(0))
    val merged = CoverageData.merge(List(first, second))
    assertEquals(merged.statements.map(_.id), List(0, 1, 2))
    assertEquals(merged.statements.map(_.sourcePath), List("A.scala", "A.scala", "B.scala"))
    assertEquals(merged.executedIds, Set(1, 2))

  test("merge of no parts is empty"):
    assertEquals(CoverageData.merge(Nil), CoverageData.Empty)
end CoverageMergeTest
