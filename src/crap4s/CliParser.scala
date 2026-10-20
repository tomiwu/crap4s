package crap4s

import java.nio.file.Files
import java.nio.file.Path
import scala.util.Try

object CliParser:

  def parse(args: List[String]): Either[Error, CliConfig] =
    readFlags(args).flatMap(toConfig)

  private case class Flags(
      tasty: List[String],
      coverage: List[String],
      classpath: List[String],
      threshold: Double,
      all: Boolean
  )

  private def readFlags(args: List[String]): Either[Error, Flags] =
    parseLoop(args, Flags(Nil, Nil, Nil, CliConfig.DefaultThreshold, false))

  private type Step = Either[Error, (List[String], Flags)]

  private val valueOptions: Map[String, (Flags, String) => Either[Error, Flags]] = Map(
    "--tasty" -> ((flags, value) => Right(flags.copy(tasty = flags.tasty :+ value))),
    "--coverage" -> ((flags, value) => Right(flags.copy(coverage = flags.coverage :+ value))),
    "--classpath" ->
      ((flags, value) => Right(flags.copy(classpath = flags.classpath ++ splitEntries(value)))),
    "--classpath-file" ->
      ((flags, value) =>
        readClasspathFile(value).map(entries => flags.copy(classpath = flags.classpath ++ entries))
      ),
    "--threshold" ->
      ((flags, value) =>
        parseThreshold(value).map(threshold => flags.copy(threshold = threshold))
      )
  )

  private def parseLoop(rest: List[String], flags: Flags): Either[Error, Flags] =
    rest match
      case Nil => Right(flags)
      case head :: tail =>
        step(head, tail, flags).flatMap((remaining, next) => parseLoop(remaining, next))

  private def step(head: String, tail: List[String], flags: Flags): Step =
    (head, tail) match
      case ("--all", _) => Right((tail, flags.copy(all = true)))
      case (option, value :: remaining) =>
        valueOptions.get(option).fold(unknown(option))(_(flags, value).map((remaining, _)))
      case _ => unknown(head)

  private def unknown(option: String): Step = Left(Error.UnknownOption(option))

  private def parseThreshold(value: String): Either[Error, Double] =
    value.toDoubleOption.toRight(Error.InvalidThreshold(value))

  /** Entries are separated by `:` or by line breaks. Empty entries are ignored. */
  def splitEntries(text: String): List[String] =
    text.split("[:\\r\\n]").toList.map(_.trim).filter(_.nonEmpty)

  private def readClasspathFile(file: String): Either[Error, List[String]] =
    Try(Files.readString(Path.of(file))).toEither.left
      .map(e => Error.CannotRead(file, e.getMessage))
      .map(splitEntries)

  private def required(option: String, values: List[String]): Either[Error, List[String]] =
    if values.isEmpty then Left(Error.MissingOption(option)) else Right(values)

  private def toConfig(flags: Flags): Either[Error, CliConfig] =
    for
      tasty <- required("--tasty", flags.tasty)
      coverage <- required("--coverage", flags.coverage)
      _ <- requireDirectories(tasty ++ coverage)
    yield CliConfig(tasty, coverage, flags.classpath, flags.threshold, flags.all)

  private def requireDirectories(paths: List[String]): Either[Error, Unit] =
    paths.find(path => !Files.isDirectory(Path.of(path))) match
      case Some(path) => Left(Error.MissingDirectory(path))
      case None => Right(())
end CliParser
