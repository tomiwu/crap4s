package crap4s.input.scoverage

import crap4s.core.Error
import crap4s.core.Statement
import crap4s.pipeline.Coverage

import java.nio.file.Path

object ScoverageSource:
  /** Builds the source from path texts. */
  def at(dirs: List[String], baseDir: String): ScoverageSource =
    ScoverageSource(dirs.map(Path.of(_)), Path.of(baseDir))

/** Reads the statements from the `-coverage-out` directories of the Scala 3 compiler. */
final class ScoverageSource(dirs: List[Path], baseDir: Path) extends Coverage:

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
