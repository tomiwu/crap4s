package crap4s.cli

import crap4s.core.Error
import crap4s.core.Threshold
import crap4s.input.InputFiles
import crap4s.input.fileSystem

def parseArgs(args: List[String], files: InputFiles = fileSystem): Either[Error, CliConfig] =
  readFlags(args, files).flatMap(toConfig(_, files))

private case class Flags(
    tasty: List[String],
    coverage: List[String],
    classpath: List[String],
    threshold: Threshold,
    all: Boolean,
    format: Format
)

private def readFlags(args: List[String], files: InputFiles): Either[Error, Flags] =
  parseLoop(
    args,
    Flags(Nil, Nil, Nil, Threshold.Default, false, defaultFormat),
    valueOptions(files)
  )

private type Step = Either[Error, (List[String], Flags)]

private def valueOptions(files: InputFiles)
    : Map[String, (Flags, String) => Either[Error, Flags]] =
  Map(
    "--tasty" -> ((flags, value) => Right(flags.copy(tasty = flags.tasty :+ value))),
    "--coverage" -> ((flags, value) => Right(flags.copy(coverage = flags.coverage :+ value))),
    "--classpath" ->
      ((flags, value) => Right(flags.copy(classpath = flags.classpath ++ splitEntries(value)))),
    "--classpath-file" ->
      ((flags, value) =>
        files.readString(value).map(splitEntries).map(entries =>
          flags.copy(classpath = flags.classpath ++ entries)
        )
      ),
    "--format" ->
      ((flags, value) => parseFormat(value).map(format => flags.copy(format = format))),
    "--threshold" ->
      ((flags, value) =>
        parseThreshold(value).map(threshold => flags.copy(threshold = threshold))
      )
  )

private type Options = Map[String, (Flags, String) => Either[Error, Flags]]

private def parseLoop(rest: List[String], flags: Flags, options: Options): Either[Error, Flags] =
  rest match
    case Nil => Right(flags)
    case head :: tail =>
      step(head, tail, flags, options).flatMap((remaining, next) =>
        parseLoop(remaining, next, options)
      )

private def step(head: String, tail: List[String], flags: Flags, options: Options): Step =
  (head, tail) match
    case ("--all", _) => Right((tail, flags.copy(all = true)))
    case (option, value :: remaining) =>
      options.get(option).fold(unknown(option))(_(flags, value).map((remaining, _)))
    case _ => unknown(head)

private def unknown(option: String): Step = Left(Error.UnknownOption(option))

private def parseThreshold(value: String): Either[Error, Threshold] =
  value.toDoubleOption
    .toRight(Error.InvalidThreshold(value))
    .flatMap(raw => Threshold.from(raw).left.map(_ => Error.InvalidThreshold(value)))

/** Entries are separated by `:` or by line breaks. Empty entries are ignored. */
def splitEntries(text: String): List[String] =
  text.split("[:\\r\\n]").toList.map(_.trim).filter(_.nonEmpty)

private def required(option: String, values: List[String]): Either[Error, List[String]] =
  if values.isEmpty then Left(Error.MissingOption(option)) else Right(values)

private def toConfig(flags: Flags, files: InputFiles): Either[Error, CliConfig] =
  for
    tasty <- required("--tasty", flags.tasty)
    coverage <- required("--coverage", flags.coverage)
    _ <- requireDirectories(tasty ++ coverage, files)
  yield CliConfig(tasty, coverage, flags.classpath, flags.threshold, flags.all, flags.format)

private def requireDirectories(paths: List[String], files: InputFiles): Either[Error, Unit] =
  paths.find(path => !files.isDirectory(path)) match
    case Some(path) => Left(Error.MissingDirectory(path))
    case None => Right(())
