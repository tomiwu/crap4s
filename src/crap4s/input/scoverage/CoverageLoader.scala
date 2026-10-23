package crap4s.input.scoverage

import crap4s.core.Error
import crap4s.core.attempt
import crap4s.core.describe

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.Using

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
  attempt(Files.readString(file))(e => Error.CannotRead(file.toString, describe(e)))

private def loadReport(
    file: Path,
    baseDir: Option[Path]
): Either[Error, List[CoverageStatement]] =
  read(file).flatMap(parseCoverageReport(_, baseDir))

private[scoverage] def measurementFiles(dir: Path): Either[Error, List[Path]] =
  attempt(Using.resource(Files.list(dir))(_.iterator.asScala.toList))(e =>
    Error.CannotList(dir.toString, describe(e))
  ).map(_.filter(_.getFileName.toString.startsWith(MeasurementsPrefix)).sorted)

private def loadMeasurements(dir: Path): Either[Error, Set[Int]] =
  measurementFiles(dir).flatMap:
    _.foldLeft(Right(Set.empty): Either[Error, Set[Int]]): (acc, file) =>
      for
        ids <- acc
        text <- read(file)
        more <- parseMeasurements(file.getFileName.toString, text)
      yield ids ++ more
