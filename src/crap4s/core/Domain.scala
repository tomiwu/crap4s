package crap4s.core

/** A value that a smart constructor rejects. */
enum ValidationError(val message: String):
  case InvalidComplexity(value: Int)
      extends ValidationError(s"Complexity must be 1 or more: $value")
  case InvalidCoverage(value: Double)
      extends ValidationError(s"Coverage must be from 0.0 to 1.0: $value")
  case InvalidThreshold(value: Double)
      extends ValidationError(s"Threshold must be more than 0: $value")
  case InvalidSourceLine(value: Int)
      extends ValidationError(s"Source line must be 1 or more: $value")
  case BlankUnitName extends ValidationError("Unit name must not be empty")

/** Cyclomatic complexity. It is 1 or more. */
opaque type Complexity = Int

object Complexity:
  def from(raw: Int): Either[ValidationError, Complexity] =
    Either.cond(raw >= 1, raw, ValidationError.InvalidComplexity(raw))

  extension (cc: Complexity) def value: Int = cc

/** Share of executed statements, from 0.0 to 1.0. */
opaque type Coverage = Double

object Coverage:
  def from(raw: Double): Either[ValidationError, Coverage] =
    Either.cond(raw >= 0.0 && raw <= 1.0, raw, ValidationError.InvalidCoverage(raw))

  extension (cov: Coverage) def value: Double = cov

/** A limit for the CRAP score. It is more than 0. */
opaque type Threshold = Double

object Threshold:
  val Default: Threshold = 6.0

  def from(raw: Double): Either[ValidationError, Threshold] =
    Either.cond(raw > 0.0, raw, ValidationError.InvalidThreshold(raw))

  extension (threshold: Threshold) def value: Double = threshold

/** A line number in a source file. It is 1 or more. */
opaque type SourceLine = Int

object SourceLine:
  def from(raw: Int): Either[ValidationError, SourceLine] =
    Either.cond(raw >= 1, raw, ValidationError.InvalidSourceLine(raw))

  extension (line: SourceLine) def value: Int = line

/** The name of a unit. It is not blank. */
opaque type UnitName = String

object UnitName:
  def from(raw: String): Either[ValidationError, UnitName] =
    Either.cond(raw.trim.nonEmpty, raw, ValidationError.BlankUnitName)

  extension (name: UnitName) def value: String = name

/** A CRAP score. Only `crapOf` makes it. */
opaque type CrapScore = Double

object CrapScore:
  extension (score: CrapScore)
    def value: Double = score
    def isAbove(threshold: Threshold): Boolean = score > threshold.value

/** CRAP = cc² × (1 − cov)³ + cc. */
def crapOf(cc: Complexity, cov: Coverage): CrapScore =
  val ccD = 1.0 * cc
  ccD * ccD * math.pow(1.0 - cov, 3) + ccD
