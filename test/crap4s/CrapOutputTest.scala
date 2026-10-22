package crap4s

import munit.FunSuite

class CrapOutputTest extends FunSuite:

  private def valid[A](result: Either[ValidationError, A]): A =
    result.fold(e => fail(e.message), identity)

  private def row(unit: String, cc: Int, cov: Double): UnitRow =
    UnitRow(
      "A.scala",
      valid(SourceLine.from(1)),
      valid(UnitName.from(unit)),
      valid(Complexity.from(cc)),
      valid(Coverage.from(cov))
    )

  private def threshold(raw: Double): Threshold = valid(Threshold.from(raw))

  private def crap(cc: Int, cov: Double): Double =
    Crap.of(valid(Complexity.from(cc)), valid(Coverage.from(cov))).value

  test("cc=1, cov=1 gives 1"):
    assertEqualsDouble(crap(1, 1.0), 1.0, 1e-9)

  test("cc=5, cov=0 gives 30"):
    assertEqualsDouble(crap(5, 0.0), 30.0, 1e-9)

  test("cc=2, cov=0.5 gives 2.5"):
    assertEqualsDouble(crap(2, 0.5), 2.5, 1e-9)

  test("rows are sorted by crap, highest first"):
    val rows = List(row("a", 1, 1.0), row("b", 5, 0.0), row("c", 2, 0.5))
    assertEquals(
      UnitRow.select(rows, threshold(0.1), all = true).map(_.unit.value),
      List("b", "c", "a")
    )

  test("default selection keeps only rows above the threshold"):
    val rows = List(row("a", 1, 1.0), row("b", 5, 0.0))
    assertEquals(UnitRow.select(rows, threshold(6.0), all = false).map(_.unit.value), List("b"))

  test("a row equal to the threshold is not above it"):
    assertEquals(
      UnitRow.select(List(row("a", 1, 1.0)), threshold(1.0), all = false),
      Nil
    )

  test("--all keeps every row"):
    assertEquals(
      UnitRow.select(List(row("a", 1, 1.0)), threshold(6.0), all = true).size,
      1
    )

  test("exceeds is true only when a row is above the threshold"):
    assert(UnitRow.exceeds(List(row("b", 5, 0.0)), threshold(6.0)))
    assert(!UnitRow.exceeds(List(row("a", 1, 1.0)), threshold(6.0)))

  test("header and plain row"):
    assertEquals(
      CsvWriter.render(List(row("a", 2, 0.5))),
      "file,line,unit,cc,cov,crap\nA.scala,1,a,2,0.50,2.50\n"
    )

  test("fields with commas and quotes are escaped"):
    assertEquals(CsvWriter.escape("a,b"), "\"a,b\"")
    assertEquals(CsvWriter.escape("say \"hi\""), "\"say \"\"hi\"\"\"")
    assertEquals(CsvWriter.escape("plain"), "plain")
    assertEquals(CsvWriter.escape("a\nb"), "\"a\nb\"")
end CrapOutputTest
