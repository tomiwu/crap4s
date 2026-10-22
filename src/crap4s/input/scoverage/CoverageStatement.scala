package crap4s.input.scoverage

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

object CoverageData:
  val Empty: CoverageData = CoverageData(Nil, Set.empty)

  /**
   * Statement ids restart at 0 in each compilation. Shift the ids of each part to keep them unique.
   */
  def merge(parts: List[CoverageData]): CoverageData =
    parts.foldLeft(Empty)(append)

  private def append(acc: CoverageData, next: CoverageData): CoverageData =
    val offset = acc.statements.map(_.id + 1).maxOption.getOrElse(0)
    CoverageData(
      acc.statements ++ next.statements.map(s => s.copy(id = s.id + offset)),
      acc.executedIds ++ next.executedIds.map(_ + offset)
    )
end CoverageData
