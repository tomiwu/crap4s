package crap4s.cli

import crap4s.core.Error

/** An output format. `name` is the value of the `--format` flag. */
enum Format(val name: String):
  case Agent extends Format("agent")
  case Human extends Format("human")

val defaultFormat: Format = Format.Agent

def parseFormat(name: String): Either[Error, Format] =
  Format.values
    .find(_.name == name)
    .toRight(Error.UnknownFormat(name, Format.values.toList.map(_.name).sorted))
