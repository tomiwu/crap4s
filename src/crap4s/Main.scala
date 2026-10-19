package crap4s

@main def crap(args: String*): Unit =
  CliParser.parse(args.toList) match
    case Left(message) =>
      Console.err.println(message)
      sys.exit(1)
    case Right(config) =>
      if Crap4sDebug.enabled then println(config.debugLine)
      sys.exit(0)

private object Crap4sDebug:
  def enabled: Boolean =
    Option(System.getenv("CRAP4S_DEBUG"))
      .exists(v => v != "0" && !v.equalsIgnoreCase("false"))
