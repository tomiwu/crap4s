package crap4s.coverage

import java.nio.file.Path

/** Parser for `scoverage.coverage`, format version 3.0.
  *
  * The format comes from the Scala 3 compiler, file
  * `compiler/src/dotty/tools/dotc/coverage/Serializer.scala`. See
  * `docs/coverage-format.md`.
  */
object CoverageReportParser:
  private val RecordSeparator = "\f"
  private val FieldCount = 15
  private val SupportedVersion = "3.0"
  private val VersionPrefix = "# Coverage data, format version: "

  def parse(text: String, baseDir: Option[Path] = None): Either[String, List[CoverageStatement]] =
    val lines = text.linesIterator.toList
    checkVersion(lines).flatMap(_ => parseRecords(lines, baseDir))

  private def checkVersion(lines: List[String]): Either[String, Unit] =
    lines.headOption.map(_.stripPrefix(VersionPrefix)) match
      case Some(SupportedVersion) if lines.head.startsWith(VersionPrefix) => Right(())
      case _ => Left(s"Damaged scoverage.coverage: expected '$VersionPrefix$SupportedVersion' on line 1")

  private def parseRecords(
      lines: List[String],
      baseDir: Option[Path]
  ): Either[String, List[CoverageStatement]] =
    splitRecords(lines.dropWhile(_.startsWith("#"))).zipWithIndex.foldLeft(
      Right(Nil): Either[String, List[CoverageStatement]]
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
  ): Either[String, CoverageStatement] =
    if record.size < FieldCount then
      Left(s"Damaged scoverage.coverage: record $number has ${record.size} lines, expected at least $FieldCount")
    else
      toStatement(record, baseDir).left.map(reason => s"Damaged scoverage.coverage: record $number: $reason")

  private def toStatement(record: List[String], baseDir: Option[Path]): Either[String, CoverageStatement] =
    for
      id <- int(record(0), "id")
      start <- int(record(7), "start offset")
      end <- int(record(8), "end offset")
      line <- int(record(9), "line")
      branch <- bool(record(12), "is branch")
    yield CoverageStatement(id, SourcePathNormalizer.normalize(record(1), baseDir), start, end, line, record(6), branch)

  private def int(value: String, field: String): Either[String, Int] =
    value.toIntOption.toRight(s"$field is not a number: '$value'")

  private def bool(value: String, field: String): Either[String, Boolean] =
    value.toBooleanOption.toRight(s"$field is not true or false: '$value'")
