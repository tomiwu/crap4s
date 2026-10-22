package crap4s.cli

import crap4s.core.Error

/** An output format. `name` is the value of the `--format` flag. */
enum Format(val name: String):
  case Agent extends Format("agent")
  case Human extends Format("human")

object Format:
  val Default: Format = Agent

  def parse(name: String): Either[Error, Format] =
    values
      .find(_.name == name)
      .toRight(Error.UnknownFormat(name, values.toList.map(_.name).sorted))
