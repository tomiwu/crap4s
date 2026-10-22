package crap4s

import munit.FunSuite

import scala.compiletime.testing.typeCheckErrors

class DomainTest extends FunSuite:

  private def rejected[A](result: Either[ValidationError, A]): Unit =
    assert(result.isLeft, clue(result))
    assert(result.left.exists(_.message.nonEmpty))

  test("Complexity rejects 0 and negative values"):
    rejected(Complexity.from(0))
    rejected(Complexity.from(-3))

  test("Complexity accepts 1"):
    assertEquals(Complexity.from(1).map(_.value), Right(1))

  test("Coverage rejects values below 0, above 1 and NaN"):
    rejected(Coverage.from(-0.01))
    rejected(Coverage.from(1.01))
    rejected(Coverage.from(Double.NaN))

  test("Coverage accepts 0.0 and 1.0"):
    assertEquals(Coverage.from(0.0).map(_.value), Right(0.0))
    assertEquals(Coverage.from(1.0).map(_.value), Right(1.0))

  test("Threshold rejects 0, negative values and NaN"):
    rejected(Threshold.from(0.0))
    rejected(Threshold.from(-1.0))
    rejected(Threshold.from(Double.NaN))

  test("Threshold accepts a positive value"):
    assertEquals(Threshold.from(0.5).map(_.value), Right(0.5))

  test("SourceLine rejects 0 and negative values"):
    rejected(SourceLine.from(0))
    rejected(SourceLine.from(-1))

  test("SourceLine accepts 1"):
    assertEquals(SourceLine.from(1).map(_.value), Right(1))

  test("UnitName rejects empty and blank names"):
    rejected(UnitName.from(""))
    rejected(UnitName.from("  \t"))

  test("UnitName accepts a name"):
    assertEquals(UnitName.from("a.B.c").map(_.value), Right("a.B.c"))

  test("each error has a message that names the value"):
    assertEquals(ValidationError.InvalidComplexity(0).message, "Complexity must be 1 or more: 0")
    assert(ValidationError.BlankUnitName.message.contains("empty"))

  test("Crap.of applies cc² × (1 − cov)³ + cc"):
    val score =
      for
        cc <- Complexity.from(2)
        cov <- Coverage.from(0.5)
      yield Crap.of(cc, cov).value
    assertEquals(score, Right(2.5))

  test("a raw Int cannot be a Complexity"):
    assert(typeCheckErrors("val c: crap4s.Complexity = 3").nonEmpty)

  test("a raw Double cannot be a Coverage or a CrapScore"):
    assert(typeCheckErrors("val c: crap4s.Coverage = 0.5").nonEmpty)
    assert(typeCheckErrors("val s: crap4s.CrapScore = 1.0").nonEmpty)

  test("there is no unchecked constructor"):
    assert(typeCheckErrors("crap4s.Complexity(3)").nonEmpty)
    assert(typeCheckErrors("crap4s.CrapScore.from(3.0)").nonEmpty)
end DomainTest
