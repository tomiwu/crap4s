package crap4s.coverage

final case class CoverageStatement(
    id: Int,
    sourcePath: String,
    startOffset: Int,
    endOffset: Int,
    line: Int,
    methodName: String,
    isBranch: Boolean
)

final case class CoverageData(
    statements: List[CoverageStatement],
    executedIds: Set[Int]
)
