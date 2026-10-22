package crap4s.core

/** A unit (method or lambda) with its place in a source file. `file` is a normalized path. */
final case class CodeUnit(
    file: String,
    startOffset: Int,
    endOffset: Int,
    line: SourceLine,
    name: UnitName,
    cc: Complexity
)

/** A statement of the coverage report. `file` is a normalized path. */
final case class Statement(file: String, startOffset: Int, endOffset: Int, executed: Boolean)
