package crap4s.input.scoverage

import crap4s.core.Error
import crap4s.core.Statement
import crap4s.core.traverse
import crap4s.input.tasty.parsePath
import crap4s.pipeline.Coverage

import java.nio.file.Path

/** Reads the statements from the `-coverage-out` directories of the Scala 3 compiler. */
def scoverageAt(dirs: List[String], baseDir: String): Coverage =
  Coverage(() =>
    for
      base <- parsePath(baseDir)
      paths <- traverse(dirs)(parsePath)
      result <- statements(paths, base)
    yield result
  )

private def statements(dirs: List[Path], baseDir: Path): Either[Error, List[Statement]] =
  loadAllCoverage(dirs, Some(baseDir)).flatMap(toStatements(_, baseDir))

private def toStatements(data: CoverageData, baseDir: Path): Either[Error, List[Statement]] =
  traverse(data.statements): s =>
    normalizePath(s.sourcePath, Some(baseDir)).map(path =>
      Statement(path, s.startOffset, s.endOffset, data.executedIds.contains(s.id))
    )
