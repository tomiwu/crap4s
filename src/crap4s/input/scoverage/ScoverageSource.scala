package crap4s.input.scoverage

import crap4s.core.Error
import crap4s.core.Statement
import crap4s.pipeline.Coverage

import java.nio.file.Path

/** Reads the statements from the `-coverage-out` directories of the Scala 3 compiler. */
object ScoverageSource:
  /** Builds the capability from path texts. */
  def at(dirs: List[String], baseDir: String): Coverage =
    val base = Path.of(baseDir)
    Coverage(() => statements(dirs.map(Path.of(_)), base))

  private def statements(dirs: List[Path], baseDir: Path): Either[Error, List[Statement]] =
    CoverageLoader.loadAll(dirs, Some(baseDir)).map(toStatements(_, baseDir))

  private def toStatements(data: CoverageData, baseDir: Path): List[Statement] =
    data.statements.map: s =>
      Statement(
        SourcePathNormalizer.normalize(s.sourcePath, Some(baseDir)),
        s.startOffset,
        s.endOffset,
        data.executedIds.contains(s.id)
      )
end ScoverageSource
