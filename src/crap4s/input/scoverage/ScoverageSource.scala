package crap4s.input.scoverage

import crap4s.core.Error
import crap4s.core.Statement
import crap4s.pipeline.Coverage

import java.nio.file.Path

/** Reads the statements from the `-coverage-out` directories of the Scala 3 compiler. */
def scoverageAt(dirs: List[String], baseDir: String): Coverage =
  val base = Path.of(baseDir)
  Coverage(() => statements(dirs.map(Path.of(_)), base))

private def statements(dirs: List[Path], baseDir: Path): Either[Error, List[Statement]] =
  loadAllCoverage(dirs, Some(baseDir)).map(toStatements(_, baseDir))

private def toStatements(data: CoverageData, baseDir: Path): List[Statement] =
  data.statements.map: s =>
    Statement(
      normalizePath(s.sourcePath, Some(baseDir)),
      s.startOffset,
      s.endOffset,
      data.executedIds.contains(s.id)
    )
