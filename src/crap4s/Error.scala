package crap4s

enum Error(val message: String):
  case UnknownOption(option: String) extends Error(s"Unknown option: $option")
  case InvalidThreshold(value: String) extends Error(s"Invalid threshold: $value")
  case MissingOption(option: String) extends Error(s"Missing required option: $option")
  case MissingDirectory(path: String) extends Error(s"Directory does not exist: $path")
  case UnresolvedSymbol(detail: String)
      extends Error(
        s"A symbol does not resolve: $detail. Add the dependencies of the project with " +
          "--classpath or --classpath-file. See docs/classpath.md."
      )
  case CannotRead(path: String, reason: String) extends Error(s"Cannot read $path: $reason")
  case CannotList(path: String, reason: String) extends Error(s"Cannot list $path: $reason")
  case BadReportVersion(expected: String)
      extends Error(s"Damaged scoverage.coverage: expected '$expected' on line 1")
  case ShortRecord(number: Int, size: Int, expected: Int)
      extends Error(
        s"Damaged scoverage.coverage: record $number has $size lines, expected at least $expected"
      )
  case BadField(
      number: Int,
      field: String,
      expected: String,
      value: String
  ) extends Error(s"Damaged scoverage.coverage: record $number: $field is not $expected: '$value'")
  case BadMeasurement(name: String, line: Int, text: String)
      extends Error(s"Damaged $name: line $line is not a statement id: '$text'")
end Error
