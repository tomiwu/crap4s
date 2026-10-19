package crap4s

@main def crap(args: String*): Unit =
  CliParser.parse(args.toList) match
    case Left(message) =>
      Console.err.println(message)
      sys.exit(1)
    case Right(config) =>
      if isDebugMode then println(config.debugLine)
      sys.exit(0)

private def isDebugMode: Boolean =
  val value = System.getenv("CRAP4S_DEBUG")
  value != null && value != "0" && !value.equalsIgnoreCase("false")
