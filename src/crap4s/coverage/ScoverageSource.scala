package crap4s.coverage

import crap4s.CoverageSource
import crap4s.Error
import crap4s.Statement

import java.nio.file.Path

/** Reads the statements from the `-coverage-out` directories of the Scala 3 compiler. */
final class ScoverageSource(dirs: List[Path], baseDir: Path) extends CoverageSource:

  def statements(): Either[Error, List[Statement]] =
    CoverageLoader.loadAll(dirs, Some(baseDir)).map(toStatements)

  private def toStatements(data: CoverageData): List[Statement] =
    data.statements.map: s =>
      Statement(
        SourcePathNormalizer.normalize(s.sourcePath, Some(baseDir)),
        s.startOffset,
        s.endOffset,
        data.executedIds.contains(s.id)
      )
end ScoverageSource
