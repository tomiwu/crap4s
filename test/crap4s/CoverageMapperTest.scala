package crap4s

import crap4s.coverage.*
import crap4s.tasty.*
import munit.FunSuite

import java.nio.file.Path

class CoverageMapperTest extends FunSuite:

  private def unit(name: String, start: Int, end: Int, kind: UnitKind = UnitKind.Method) =
    LoadedMethod("O", name, MethodPosition("/p/src/A.scala", start, end, 1), None, kind)

  private def stmt(id: Int, start: Int, end: Int, path: String = "src/A.scala") =
    CoverageStatement(id, path, start, end, 1, "m", false)

  private val base = Some(Path.of("/p"))

  private def covs(units: List[LoadedMethod], stmts: List[CoverageStatement], run: Set[Int]) =
    CoverageMapper.map(units, CoverageData(stmts, run), base).map(_.cov)

  test("fully covered method is 1.0"):
    assertEquals(covs(List(unit("f", 0, 50)), List(stmt(1, 5, 10), stmt(2, 10, 20)), Set(1, 2)), List(1.0))

  test("method that is not called is 0.0"):
    assertEquals(covs(List(unit("f", 0, 50)), List(stmt(1, 5, 10)), Set.empty), List(0.0))

  test("partly covered method is between 0 and 1"):
    assertEquals(covs(List(unit("f", 0, 50)), List(stmt(1, 5, 10), stmt(2, 10, 20)), Set(1)), List(0.5))

  test("a lambda takes its statements before the parent method"):
    val units = List(unit("f", 0, 100), unit("lam", 20, 40, UnitKind.Lambda))
    val result = CoverageMapper.map(units, CoverageData(List(stmt(1, 5, 10), stmt(2, 25, 30)), Set(2)), base)
    assertEquals(result.map(r => (r.total, r.executed)), List((1, 0), (1, 1)))

  test("range end is exclusive of later offsets"):
    assertEquals(CoverageMapper.map(List(unit("f", 0, 10)), CoverageData(List(stmt(1, 5, 11)), Set(1)), base).map(_.total), List(0))

  test("statements of other files are ignored"):
    assertEquals(CoverageMapper.map(List(unit("f", 0, 50)), CoverageData(List(stmt(1, 5, 10, "src/B.scala")), Set(1)), base).map(_.total), List(0))

  test("a unit without statements has the default coverage"):
    assertEquals(covs(List(unit("f", 0, 50)), Nil, Set.empty), List(CoverageMapper.NoStatementsCoverage))
    assertEquals(CoverageMapper.NoStatementsCoverage, 0.0)
