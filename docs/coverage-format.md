# Format of the `-coverage-out` directory

Source: the Scala 3 compiler, file
`compiler/src/dotty/tools/dotc/coverage/Serializer.scala`
(<https://github.com/scala/scala3/blob/3.10.0/compiler/src/dotty/tools/dotc/coverage/Serializer.scala>).
Version in use: Scala 3.10.0. The report says `format version: 3.0`.

## `scoverage.coverage`

- Comment lines start with `#`. Line 1 is `# Coverage data, format version: 3.0`.
- Each statement is a record. A line with only a form feed (`\f`) ends each record.
- A record has these lines, in this order:
  id, source path, package, class, class type, full class name, method name,
  start offset, end offset, line, symbol name, tree name, is branch,
  invocations count, is ignored, description.
- The description can use more than one line.
- The report has no base directory header. The source path is relative to
  the `-sourceroot` of the compiler. crap4s makes the path normalized (`/` separators, no `.` or `..`).
  If the caller gives a base directory, an absolute path inside it becomes relative to it.

## `scoverage.measurements.*`

- There can be many files. A file has one statement id on each line.
- A statement is executed if its id is in one of the files.
