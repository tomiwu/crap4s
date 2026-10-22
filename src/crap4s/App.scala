package crap4s

import crap4s.cli.CliConfig
import crap4s.cli.ExitCode
import crap4s.cli.parseArgs
import crap4s.cli.reportFor
import crap4s.cli.usageText
import crap4s.core.Error
import crap4s.core.mapCoverage
import crap4s.core.rowsFromStats
import crap4s.input.InputFiles
import crap4s.input.fileSystem
import crap4s.input.scoverage.scoverageAt
import crap4s.input.tasty.tastyUnitsAt
import crap4s.pipeline.Coverage
import crap4s.pipeline.Inputs
import crap4s.pipeline.Mapped
import crap4s.pipeline.Mapping
import crap4s.pipeline.Report
import crap4s.pipeline.Result
import crap4s.pipeline.Scored
import crap4s.pipeline.Scoring
import crap4s.pipeline.Units
import crap4s.pipeline.pipeline

final case class Outcome(out: String, err: String, code: ExitCode)

/** Wires the modules from the parsed config. */
object App:
  def run(
      args: List[String],
      baseDir: String,
      color: Boolean = false,
      files: InputFiles = fileSystem
  ): Outcome =
    if args.contains("--help") then Outcome(usageText, "", ExitCode.Pass)
    else
      parseArgs(args, files)
        .flatMap(execute(_, baseDir, color, files))
        .fold(failure, identity)

  def failure(error: Error): Outcome = Outcome("", error.message + "\n", ExitCode.Error)

  private def execute(
      config: CliConfig,
      baseDir: String,
      color: Boolean,
      files: InputFiles
  ): Either[Error, Outcome] =
    val (entries, missing) = config.classpath.partition(files.exists)
    given Units = tastyUnitsAt(config.tastyDirs, entries, baseDir)
    given Coverage = scoverageAt(config.coverageDirs, baseDir)
    given Mapping = Mapping(mapInputs)
    given Scoring = Scoring(scoreStats)
    given Report = reportFor(config.format, config.threshold, config.all, color)
    pipeline.map(outcome(_, missing))

  private def mapInputs(inputs: Inputs): Either[Error, Mapped] =
    Right(Mapped(mapCoverage(inputs.units, inputs.statements)))

  private def scoreStats(mapped: Mapped): Either[Error, Scored] =
    rowsFromStats(mapped.stats).map(Scored(_))

  private def outcome(result: Result, missing: List[String]): Outcome =
    Outcome(result.out, missing.map(warning).mkString, result.code)

  private def warning(entry: String): String =
    s"Warning: classpath entry does not exist: ${entry}\n"
end App
