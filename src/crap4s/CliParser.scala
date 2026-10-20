package crap4s

import java.nio.file.Files
import java.nio.file.Path

object CliParser:

  def parse(args: List[String]): Either[Error, CliConfig] =
    readFlags(args).flatMap(toConfig)

  private case class Flags(
      tasty: Option[String],
      coverage: Option[String],
      threshold: Double,
      all: Boolean
  )

  private def readFlags(args: List[String]): Either[Error, Flags] =
    parseLoop(args, Flags(None, None, CliConfig.DefaultThreshold, false))

  private type Step = Either[Error, (List[String], Flags)]

  private val valueOptions: Map[String, (Flags, String) => Either[Error, Flags]] = Map(
    "--tasty" -> ((flags, value) => Right(flags.copy(tasty = Some(value)))),
    "--coverage" -> ((flags, value) => Right(flags.copy(coverage = Some(value)))),
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

  private def toConfig(flags: Flags): Either[Error, CliConfig] =
    for
      tasty <- flags.tasty.toRight(Error.MissingOption("--tasty"))
      coverage <- flags.coverage.toRight(Error.MissingOption("--coverage"))
      _ <- requireDirectory(tasty)
      _ <- requireDirectory(coverage)
    yield CliConfig(tasty, coverage, flags.threshold, flags.all)

  private def requireDirectory(path: String): Either[Error, Unit] =
    if Files.isDirectory(Path.of(path)) then Right(())
    else Left(Error.MissingDirectory(path))
end CliParser
