package crap4s

object Usage:
  val text: String =
    """Usage: crap4s --tasty <dir> --coverage <dir> [options]
      |
      |Options:
      |  --tasty <dir>            Directory with TASTy files. Repeat for more directories.
      |  --coverage <dir>         Directory with the -coverage-out data. Repeat for more directories.
      |  --classpath <entries>    Dependency classpath. Separate entries with ':'.
      |  --classpath-file <file>  File with the dependency classpath.
      |  --threshold <number>     Report units with a CRAP score above this number. Default: 6.
      |  --all                    Report every unit.
      |  --format <name>          Output format. Known formats: csv. Default: csv.
      |  --help                   Print this text.
      |
      |Exit codes: 0 = no unit above the threshold, 1 = error, 2 = a unit above the threshold.
      |""".stripMargin
