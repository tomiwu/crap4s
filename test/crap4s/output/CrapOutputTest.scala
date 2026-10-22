package crap4s.output

import crap4s.core.Complexity
import crap4s.core.Coverage
import crap4s.core.Crap
import crap4s.core.SourceLine
import crap4s.core.Threshold
import crap4s.core.UnitName
import crap4s.core.UnitRow
import crap4s.core.ValidationError
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

  private def failing = List(row("a", 1, 1.0), row("b", 5, 0.2), row("c", 2, 0.0))

  test("golden: some failing units, worst first, then FAIL summary"):
    assertEquals(
      AgentWriter.render(failing, threshold(3.0), all = false).toOption.get,
      "A.scala:1 b cc=5 cov=0.20 crap=17.80\nA.scala:1 c cc=2 cov=0.00 crap=6.00\n" +
        "FAIL 2/3 max=17.80\n"
    )

  test("golden: all units pass gives only the PASS line"):
    assertEquals(
      AgentWriter.render(
        List(row("a", 1, 1.0), row("c", 2, 0.5)),
        threshold(6.0),
        all = false
      ).toOption.get,
      "PASS 2 max=2.50\n"
    )

  test("golden: 0 units"):
    assertEquals(
      AgentWriter.render(Nil, threshold(6.0), all = false).toOption.get,
      "PASS 0 max=0.00\n"
    )

  test("--all lists every unit before the summary"):
    assertEquals(
      AgentWriter.render(List(row("a", 1, 1.0)), threshold(6.0), all = true).toOption.get,
      "A.scala:1 a cc=1 cov=1.00 crap=1.00\nPASS 1 max=1.00\n"
    )

  test("golden: a failing unit with no-cov gets the marker at the end of its line"):
    val rows = List(row("a", 5, 0.0).copy(noCov = true), row("b", 5, 0.0))
    assertEquals(
      AgentWriter.render(rows, threshold(6.0), all = false).toOption.get,
      "A.scala:1 a cc=5 cov=0.00 crap=30.00 no-cov\nA.scala:1 b cc=5 cov=0.00 crap=30.00\n" +
        "FAIL 2/2 max=30.00\n"
    )
end CrapOutputTest
