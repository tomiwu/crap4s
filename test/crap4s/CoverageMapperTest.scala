package crap4s

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

  private def stmt(start: Int, end: Int, executed: Boolean, file: String = "src/A.scala") =
    Statement(file, start, end, executed)

  private def covs(units: List[CodeUnit], stmts: List[Statement]): List[Double] =
    CoverageMapper.map(units, stmts).map(_.cov)

  test("fully covered unit is 1.0"):
    assertEquals(
      covs(List(unit("f", 0, 50)), List(stmt(5, 10, true), stmt(10, 20, true))),
      List(1.0)
    )

  test("unit that is not called is 0.0"):
    assertEquals(covs(List(unit("f", 0, 50)), List(stmt(5, 10, false))), List(0.0))

  test("partly covered unit is between 0 and 1"):
    assertEquals(
      covs(List(unit("f", 0, 50)), List(stmt(5, 10, true), stmt(10, 20, false))),
      List(0.5)
    )

  test("a nested unit takes its statements before the parent"):
    val units = List(unit("f", 0, 100), unit("lam", 20, 40))
    val result = CoverageMapper.map(units, List(stmt(5, 10, false), stmt(25, 30, true)))
    assertEquals(result.map(r => (r.total, r.executed)), List((1, 0), (1, 1)))

  test("a statement that ends after the unit is ignored"):
    assertEquals(
      CoverageMapper.map(List(unit("f", 0, 10)), List(stmt(5, 11, true))).map(_.total),
      List(0)
    )

  test("statements of other files are ignored"):
    assertEquals(
      CoverageMapper.map(
        List(unit("f", 0, 50)),
        List(stmt(5, 10, true, "src/B.scala"))
      ).map(_.total),
      List(0)
    )

  test("a unit without statements has the default coverage"):
    assertEquals(covs(List(unit("f", 0, 50)), Nil), List(CoverageMapper.NoStatementsCoverage))
    assertEquals(CoverageMapper.NoStatementsCoverage, 0.0)

  test("a unit without statements has coverage 0.0, the no-cov flag and CRAP cc² + cc"):
    val stats = CoverageMapper.map(List(unit("f", 0, 50)), Nil).head
    assertEquals(stats.cov, 0.0)
    assert(stats.noCov)
    assertEqualsDouble(
      Crap.of(stats.unit.cc, valid(Coverage.from(stats.cov))).value,
      stats.unit.cc.value * (stats.unit.cc.value + 1.0),
      1e-9
    )

  test("a unit with statements and none executed has coverage 0.0 and no flag"):
    val stats = CoverageMapper.map(List(unit("f", 0, 50)), List(stmt(5, 10, false))).head
    assertEquals(stats.cov, 0.0)
    assert(!stats.noCov)
end CoverageMapperTest
