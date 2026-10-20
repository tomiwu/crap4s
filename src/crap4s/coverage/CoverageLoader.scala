package crap4s.coverage

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.util.Try

object CoverageLoader:
  private val ReportName = "scoverage.coverage"
  private val MeasurementsPrefix = "scoverage.measurements."

  def load(dir: Path, baseDir: Option[Path] = None): Either[String, CoverageData] =
    for
      statements <- loadReport(dir.resolve(ReportName), baseDir)
      ids <- loadMeasurements(dir)
    yield CoverageData(statements, ids)

  private def read(file: Path): Either[String, String] =
    Try(Files.readString(file)).toEither.left.map(e => s"Cannot read $file: ${e.getMessage}")

  private def loadReport(
      file: Path,
      baseDir: Option[Path]
  ): Either[String, List[CoverageStatement]] =
    read(file).flatMap(CoverageReportParser.parse(_, baseDir))

  private def measurementFiles(dir: Path): Either[String, List[Path]] =
    Try(Files.list(dir).iterator.asScala.toList).toEither.left
      .map(e => s"Cannot list $dir: ${e.getMessage}")
      .map(_.filter(_.getFileName.toString.startsWith(MeasurementsPrefix)).sorted)

  private def loadMeasurements(dir: Path): Either[String, Set[Int]] =
    measurementFiles(dir).flatMap:
      _.foldLeft(Right(Set.empty): Either[String, Set[Int]]): (acc, file) =>
        for
          ids <- acc
          text <- read(file)
          more <- MeasurementsParser.parse(file.getFileName.toString, text)
        yield ids ++ more
end CoverageLoader
