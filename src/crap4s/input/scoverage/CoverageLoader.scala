package crap4s.input.scoverage

import crap4s.core.Error

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.Try

private val ReportName = "scoverage.coverage"
private val MeasurementsPrefix = "scoverage.measurements."

def loadAllCoverage(dirs: List[Path], baseDir: Option[Path] = None): Either[Error, CoverageData] =
  dirs
    .foldLeft(Right(Nil): Either[Error, List[CoverageData]]): (acc, dir) =>
      for
        parts <- acc
        part <- loadCoverage(dir, baseDir)
      yield parts :+ part
    .map(mergeCoverage)

def loadCoverage(dir: Path, baseDir: Option[Path] = None): Either[Error, CoverageData] =
  for
    statements <- loadReport(dir.resolve(ReportName), baseDir)
    ids <- loadMeasurements(dir)
  yield CoverageData(statements, ids)

private def read(file: Path): Either[Error, String] =
  Try(Files.readString(file)).toEither.left.map(e =>
    Error.CannotRead(file.toString, e.getMessage)
  )

private def loadReport(
    file: Path,
    baseDir: Option[Path]
): Either[Error, List[CoverageStatement]] =
  read(file).flatMap(parseCoverageReport(_, baseDir))

private def measurementFiles(dir: Path): Either[Error, List[Path]] =
  Try(Files.list(dir).iterator.asScala.toList).toEither.left
    .map(e => Error.CannotList(dir.toString, e.getMessage))
    .map(_.filter(_.getFileName.toString.startsWith(MeasurementsPrefix)).sorted)

private def loadMeasurements(dir: Path): Either[Error, Set[Int]] =
  measurementFiles(dir).flatMap:
    _.foldLeft(Right(Set.empty): Either[Error, Set[Int]]): (acc, file) =>
      for
        ids <- acc
        text <- read(file)
        more <- parseMeasurements(file.getFileName.toString, text)
      yield ids ++ more
