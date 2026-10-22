package crap4s

import munit.FunSuite

class HumanWriterTest extends FunSuite:

  private def valid[A](result: Either[ValidationError, A]): A =
    result.fold(e => fail(e.message), identity)

  private def row(file: String, line: Int, name: String, cc: Int, cov: Double): UnitRow =
    UnitRow(
      file,
      valid(SourceLine.from(line)),
      valid(UnitName.from(name)),
      valid(Complexity.from(cc)),
      valid(Coverage.from(cov))
    )

  private val rows = List(
    row("src/A.scala", 3, "A.f", 1, 1.0),
    row("src/LongFileName.scala", 120, "LongFileName.veryLongMethodName", 5, 0.2),
    row("src/B.scala", 10, "B.g", 2, 0.5)
  )

  private val Esc = "\u001b["

  test("golden table without colour"):
    val expected =
      """file:line                   unit                             cc   cov   crap
        |src/LongFileName.scala:120  LongFileName.veryLongMethodName   5   20%  17.80
        |src/B.scala:10              B.g                               2   50%   2.50
        |src/A.scala:3               A.f                               1  100%   1.00
        |3 units, max CRAP 17.80, 1 over threshold
        |""".stripMargin
    assertEquals(HumanWriter(false).render(rows, Threshold.Default, all = false), expected)

  test("colour marks only rows above the threshold"):
    val out = HumanWriter(true).render(rows, Threshold.Default, all = false)
    val lines = out.linesIterator.toList
    assert(!lines.head.contains(Esc))
    assert(lines(1).startsWith(s"${Esc}31m") && lines(1).endsWith(s"${Esc}0m"))
    assert(!lines(2).contains(Esc))
    assert(!lines.last.contains(Esc))

  test("no colour code appears when colour is off"):
    assert(!HumanWriter(false).render(rows, Threshold.Default, all = true).contains(Esc))

  test("columns stay aligned with long unit names"):
    val lines = HumanWriter(false).render(rows, Threshold.Default, all = true).linesIterator.toList
    val table = lines.init
    assertEquals(table.map(_.length).distinct.size, 1)
    assertEquals(table.map(_.indexOf("cc")).filter(_ >= 0).size, 1)

  test("an empty list gives only the summary"):
    assertEquals(
      HumanWriter(false).render(Nil, Threshold.Default, all = false),
      "file:line  unit  cc  cov  crap\n0 units, max CRAP 0.00, 0 over threshold\n"
    )

  test("colour is on for a terminal without NO_COLOR"):
    assert(Colour.enabled(_ => None, isTerminal = true))

  test("colour is off with NO_COLOR, even when empty"):
    assert(!Colour.enabled(name => Option.when(name == "NO_COLOR")(""), isTerminal = true))

  test("colour is off when stdout is not a terminal"):
    assert(!Colour.enabled(_ => None, isTerminal = false))

  test("the registry builds the human writer"):
    assert(Registry.writer("human").isRight)
end HumanWriterTest
