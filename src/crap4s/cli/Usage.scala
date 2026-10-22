package crap4s.cli

object Usage:
  val text: String =
    """Usage: crap4s --tasty <dir> --coverage <dir> [options]
      |
      |Options:
      |  --tasty <dir>            Directory with TASTy files. Repeat for more directories.
      |  --coverage <dir>         Directory with the -coverage-out data. Repeat for more directories.
      |  --classpath <entries>    Dependency classpath. Separate entries with ':'.
      |  --classpath-file <file>  File with the dependency classpath.
      |  --threshold <number>     Fail units with a CRAP score above this number. Default: 6.
      |  --all                    List every unit, not only the units above the threshold.
      |  --format <name>          Output format. Known formats: agent, human. Default: agent.
      |  --help                   Print this text.
      |
      |Output: one line for each listed unit, worst first, then a summary line
      |  (PASS <units> max=<crap> or FAIL <failing>/<units> max=<crap>).
      |  The human format prints an aligned table of all units, worst first, then a summary
      |  line. It marks units above the threshold in red, unless NO_COLOR is set or stdout is not a TTY.
      |Exit codes: 0 = no unit above the threshold, 1 = a unit above the threshold,
      |  2 = bad input or error (message on stderr).
      |""".stripMargin
end Usage
