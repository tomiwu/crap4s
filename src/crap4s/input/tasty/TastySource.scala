package crap4s.input.tasty

import crap4s.core.CodeUnit
import crap4s.core.Complexity
import crap4s.core.Error
import crap4s.core.SourceLine
import crap4s.core.UnitName
import crap4s.core.traverse
import crap4s.input.scoverage.normalizePath
import crap4s.pipeline.Units
import tastyquery.Contexts.Context
import tastyquery.Exceptions.InvalidProgramStructureException

import java.nio.file.Path

/** Reads the units from TASTy files. */
/** Builds the capability from path texts. */
def tastyUnitsAt(tastyDirs: List[String], classpath: List[String], baseDir: String): Units =
  val base = Path.of(baseDir)
  Units(() => units(tastyDirs.map(Path.of(_)), classpath.map(Path.of(_)), base))

private def units(
    tastyDirs: List[Path],
    classpath: List[Path],
    baseDir: Path
): Either[Error, List[CodeUnit]] =
  try loadTasty(tastyDirs, classpath).flatMap(toUnits(_, baseDir))
  catch case e: InvalidProgramStructureException => Left(Error.UnresolvedSymbol(e.getMessage))

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
