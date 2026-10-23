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
import crap4s.input.splitExisting
import crap4s.input.tasty.tastyUnitsAt
import crap4s.output.colourEnabled
import crap4s.pipeline.Coverage
import crap4s.pipeline.Mapped
import crap4s.pipeline.Mapping
import crap4s.pipeline.Report
import crap4s.pipeline.Result
import crap4s.pipeline.Scored
import crap4s.pipeline.Scoring
import crap4s.pipeline.Stage
import crap4s.pipeline.Units
import crap4s.pipeline.pipeline

final case class Outcome(out: String, err: String, code: ExitCode)

/** Wires the modules from the parsed config. */
object App:
  /** Runs the tool, writes the result to the terminal and gives the exit code. */
  def main(args: List[String])(using terminal: Terminal, env: Env): Int =
    guarded(exec(args))

  private def exec(args: List[String])(using terminal: Terminal, env: Env): Int =
    val color = colourEnabled(env.variable, terminal.isTty())
    val outcome = run(args, env.workingDir(), color)
    terminal.out(outcome.out)
    terminal.err(outcome.err)
    outcome.code.value

  def run(
      args: List[String],
      baseDir: String,
      color: Boolean = false,
      files: InputFiles = fileSystem
  )(using env: Env): Outcome =
    if args.contains("--help") then Outcome(usageText, "", ExitCode.Pass)
    else
      parseArgs(args, files)
        .flatMap(execute(_, baseDir, color, files, env.property("java.class.path").getOrElse("")))
        .fold(failure, identity)

  def failure(error: Error): Outcome = Outcome("", s"crap4s: ${error.message}\n", ExitCode.Error)

  private def execute(
      config: CliConfig,
      baseDir: String,
      color: Boolean,
      files: InputFiles,
      javaClassPath: String
  ): Either[Error, Outcome] =
    splitExisting(config.classpath, files).flatMap: (entries, missing) =>
      given Units = tastyUnitsAt(config.tastyDirs, entries, baseDir, javaClassPath)
      given Coverage = scoverageAt(config.coverageDirs, baseDir)
      given Mapping = Stage.total(in => Mapped(mapCoverage(in.units, in.statements)))
      given Scoring = Stage(scoreStats)
      given Report = reportFor(config.format, config.threshold, config.all, color)
      pipeline.map(outcome(_, missing))

  private def scoreStats(mapped: Mapped): Either[Error, Scored] =
    rowsFromStats(mapped.stats).map(Scored(_))

  private def outcome(result: Result, missing: List[String]): Outcome =
    Outcome(result.out, missing.map(warning).mkString, result.code)

  private def warning(entry: String): String =
    s"Warning: classpath entry does not exist: ${entry}\n"
end App
