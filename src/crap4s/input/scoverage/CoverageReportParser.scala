package crap4s.input.scoverage

import crap4s.core.Error

import java.nio.file.Path

/**
 * Parser for `scoverage.coverage`, format version 3.0.
 *
 * The format comes from the Scala 3 compiler, file
 * `compiler/src/dotty/tools/dotc/coverage/Serializer.scala`. See `docs/coverage-format.md`.
 */
private val RecordSeparator = "\f"
private val FieldCount = 15
private val SupportedVersion = "3.0"
private val VersionPrefix = "# Coverage data, format version: "

def parseCoverageReport(
    text: String,
    baseDir: Option[Path] = None
): Either[Error, List[CoverageStatement]] =
  val lines = text.linesIterator.toList
  checkVersion(lines).flatMap(_ => parseRecords(lines, baseDir))

private def checkVersion(lines: List[String]): Either[Error, Unit] =
  val expected = VersionPrefix + SupportedVersion
  Either.cond(
    lines.headOption.contains(expected),
    (),
    Error.BadReportVersion(expected)
  )

private def parseRecords(
    lines: List[String],
    baseDir: Option[Path]
): Either[Error, List[CoverageStatement]] =
  splitRecords(lines.dropWhile(_.startsWith("#"))).zipWithIndex.foldLeft(
    Right(Nil): Either[Error, List[CoverageStatement]]
  ):
    case (acc, (record, index)) =>
      for
        done <- acc
        statement <- parseRecord(record, index + 1, baseDir)
      yield statement :: done
  .map(_.reverse)

private def splitRecords(lines: List[String]): List[List[String]] =
  val (current, done) = lines.foldLeft((List.empty[String], List.empty[List[String]])):
    case ((cur, acc), RecordSeparator) => (Nil, cur.reverse :: acc)
    case ((cur, acc), line) => (line :: cur, acc)
  (if current.isEmpty then done else current.reverse :: done).reverse

private def parseRecord(
    record: List[String],
    number: Int,
    baseDir: Option[Path]
): Either[Error, CoverageStatement] =
  if record.size < FieldCount then
    Left(Error.ShortRecord(number, record.size, FieldCount))
  else
    toStatement(record, number, baseDir)

private def toStatement(
    record: List[String],
    number: Int,
    baseDir: Option[Path]
): Either[Error, CoverageStatement] =
  for
    id <- int(record(0), "id", number)
    start <- int(record(7), "start offset", number)
    end <- int(record(8), "end offset", number)
    line <- int(record(9), "line", number)
    branch <- bool(record(12), "is branch", number)
  yield CoverageStatement(
    id,
    normalizePath(record(1), baseDir),
    start,
    end,
    line,
    record(6),
    branch
  )

private def int(value: String, field: String, number: Int): Either[Error, Int] =
  value.toIntOption.toRight(Error.BadField(number, field, "a number", value))

private def bool(value: String, field: String, number: Int): Either[Error, Boolean] =
  value.toBooleanOption.toRight(Error.BadField(number, field, "true or false", value))
