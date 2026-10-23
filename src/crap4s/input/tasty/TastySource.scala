package crap4s.input.tasty

import crap4s.core.CodeUnit
import crap4s.core.Complexity
import crap4s.core.Error
import crap4s.core.SourceLine
import crap4s.core.UnitName
import crap4s.core.attempt
import crap4s.core.traverse
import crap4s.input.scoverage.normalizePath
import crap4s.pipeline.Units
import tastyquery.Contexts.Context

import java.nio.file.Path

/** Reads the units from TASTy files. */
def tastyUnitsAt(tastyDirs: List[String], classpath: List[String], baseDir: String): Units =
  Units(() =>
    for
      base <- parsePath(baseDir)
      dirs <- traverse(tastyDirs)(parsePath)
      entries <- traverse(classpath)(parsePath)
      units <- units(dirs, entries, base)
    yield units
  )

private def units(
    tastyDirs: List[Path],
    classpath: List[Path],
    baseDir: Path
): Either[Error, List[CodeUnit]] =
  loadTasty(tastyDirs, classpath).flatMap(tasty =>
    attempt(toUnits(tasty, baseDir))(tastyError).flatten
  )

private def toUnits(tasty: TastyLoadResult, baseDir: Path): Either[Error, List[CodeUnit]] =
  given Context = tasty.context
  traverse(tasty.methods)(methodToUnit(_, baseDir))

private def methodToUnit(method: LoadedMethod, baseDir: Path)(using
    Context
): Either[Error, CodeUnit] =
  val position = method.position
  val unit =
    for
      line <- SourceLine.from(position.line)
      name <- UnitName.from(s"${method.owner}.${method.name}")
      cc <- Complexity.from(complexityOfUnit(method))
    yield CodeUnit(
      file = normalizePath(position.sourceFile, Some(baseDir)),
      startOffset = position.startOffset,
      endOffset = position.endOffset,
      line = line,
      name = name,
      cc = cc
    )
  unit.left.map(Error.InvalidValue(_))
