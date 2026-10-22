package crap4s.cli

/** The exit code of the process. */
enum ExitCode(val value: Int):
  case Pass extends ExitCode(0)
  case OverThreshold extends ExitCode(1)
  case Error extends ExitCode(2)
