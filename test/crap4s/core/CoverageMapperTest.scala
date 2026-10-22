package crap4s.core

import munit.FunSuite

class CoverageMapperTest extends FunSuite:

  private def valid[A](result: Either[ValidationError, A]): A =
    result.fold(e => fail(e.message), identity)

  private def unit(name: String, start: Int, end: Int, file: String = "src/A.scala") =
    CodeUnit(
      file,
      start,
      end,
      valid(SourceLine.from(1)),
      valid(UnitName.from(name)),
      valid(Complexity.from(1))
    )

  private def covs(units: List[CodeUnit], stmts: List[Statement]): List[Double] =
    mapCoverage(units, stmts).map(_.cov)

  test("fully covered unit is 1.0"):
    assertEquals(
      covs(
        List(unit("f", 0, 50)),
        List(Statement("src/A.scala", 5, 10, true), Statement("src/A.scala", 10, 20, true))
      ),
      List(1.0)
    )

  test("unit that is not called is 0.0"):
    assertEquals(
      covs(List(unit("f", 0, 50)), List(Statement("src/A.scala", 5, 10, false))),
      List(0.0)
    )

  test("partly covered unit is between 0 and 1"):
    assertEquals(
      covs(
        List(unit("f", 0, 50)),
        List(Statement("src/A.scala", 5, 10, true), Statement("src/A.scala", 10, 20, false))
      ),
      List(0.5)
    )

  test("a nested unit takes its statements before the parent"):
    val units = List(unit("f", 0, 100), unit("lam", 20, 40))
    val result = mapCoverage(
      units,
      List(Statement("src/A.scala", 5, 10, false), Statement("src/A.scala", 25, 30, true))
    )
    assertEquals(result.map(r => (r.total, r.executed)), List((1, 0), (1, 1)))

  test("a statement that ends after the unit is ignored"):
    assertEquals(
      mapCoverage(List(unit("f", 0, 10)), List(Statement("src/A.scala", 5, 11, true))).map(_.total),
      List(0)
    )

  test("statements of other files are ignored"):
    assertEquals(
      mapCoverage(
        List(unit("f", 0, 50)),
        List(Statement("src/B.scala", 5, 10, true))
      ).map(_.total),
      List(0)
    )

  test("a unit without statements has the default coverage"):
    assertEquals(covs(List(unit("f", 0, 50)), Nil), List(noStatementsCoverage))
    assertEquals(noStatementsCoverage, 0.0)

  test("a unit without statements has coverage 0.0, the no-cov flag and CRAP cc² + cc"):
    val stats = mapCoverage(List(unit("f", 0, 50)), Nil).head
    assertEquals(stats.cov, 0.0)
    assert(stats.noCov)
    assertEqualsDouble(
      crapOf(stats.unit.cc, valid(Coverage.from(stats.cov))).value,
      stats.unit.cc.value * (stats.unit.cc.value + 1.0),
      1e-9
    )

  test("a unit with statements and none executed has coverage 0.0 and no flag"):
    val stats =
      mapCoverage(List(unit("f", 0, 50)), List(Statement("src/A.scala", 5, 10, false))).head
    assertEquals(stats.cov, 0.0)
    assert(!stats.noCov)
end CoverageMapperTest
