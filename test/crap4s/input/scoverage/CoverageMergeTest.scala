package crap4s.input.scoverage

import munit.FunSuite

class CoverageMergeTest extends FunSuite:

  test("merge keeps statement ids unique and shifts the executed ids"):
    val first = CoverageData(
      List(
        CoverageStatement(0, "A.scala", 0, 1, 1, "m", false),
        CoverageStatement(1, "A.scala", 0, 1, 1, "m", false)
      ),
      Set(1)
    )
    val second = CoverageData(List(CoverageStatement(0, "B.scala", 0, 1, 1, "m", false)), Set(0))
    val merged = mergeCoverage(List(first, second))
    assertEquals(merged.statements.map(_.id), List(0, 1, 2))
    assertEquals(merged.statements.map(_.sourcePath), List("A.scala", "A.scala", "B.scala"))
    assertEquals(merged.executedIds, Set(1, 2))

  test("merge of no parts is empty"):
    assertEquals(mergeCoverage(Nil), emptyCoverage)
end CoverageMergeTest
