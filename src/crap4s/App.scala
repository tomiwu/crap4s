package crap4s

import crap4s.cli.CliConfig
import crap4s.cli.CliParser
import crap4s.cli.ExitCode
import crap4s.cli.Registry
import crap4s.cli.Usage
import crap4s.core.Error
import crap4s.input.InputFiles
import crap4s.input.scoverage.ScoverageSource
import crap4s.input.tasty.TastySource
import crap4s.pipeline.Coverage
import crap4s.pipeline.Report
import crap4s.pipeline.Result
import crap4s.pipeline.Units
import crap4s.pipeline.pipeline

final case class Outcome(out: String, err: String, code: ExitCode)

/** Wires the modules from the parsed config. */
object App:
  def run(
      args: List[String],
      baseDir: String,
      color: Boolean = false,
      files: InputFiles = InputFiles
  ): Outcome =
    if args.contains("--help") then Outcome(Usage.text, "", ExitCode.Pass)
    else
      CliParser
        .parse(args, files)
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
    given Units = TastySource.at(config.tastyDirs, entries, baseDir)
    given Coverage = ScoverageSource.at(config.coverageDirs, baseDir)
    given Report = Registry.report(config.format, color)
    pipeline(config.threshold, config.all).map(outcome(_, missing))

  private def outcome(result: Result, missing: List[String]): Outcome =
    Outcome(result.out, missing.map(warning).mkString, result.code)

  private def warning(entry: String): String =
    s"Warning: classpath entry does not exist: ${entry}\n"
end App
