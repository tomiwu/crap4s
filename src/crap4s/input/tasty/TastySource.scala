package crap4s.input.tasty

import crap4s.core.CodeUnit
import crap4s.core.Complexity
import crap4s.core.Eithers
import crap4s.core.Error
import crap4s.core.SourceLine
import crap4s.core.UnitName
import crap4s.input.scoverage.SourcePathNormalizer
import crap4s.pipeline.Units
import tastyquery.Contexts.Context
import tastyquery.Exceptions.InvalidProgramStructureException

import java.nio.file.Path

object TastySource:
  /** Builds the source from path texts. */
  def at(tastyDirs: List[String], classpath: List[String], baseDir: String): TastySource =
    TastySource(tastyDirs.map(Path.of(_)), classpath.map(Path.of(_)), Path.of(baseDir))

/** Reads the units from TASTy files. */
final class TastySource(tastyDirs: List[Path], classpath: List[Path], baseDir: Path)
    extends Units:

  def units(): Either[Error, List[CodeUnit]] =
    try TastyLoader.load(tastyDirs, classpath).flatMap(toUnits)
    catch case e: InvalidProgramStructureException => Left(Error.UnresolvedSymbol(e.getMessage))

  private def toUnits(tasty: TastyLoadResult): Either[Error, List[CodeUnit]] =
    given Context = tasty.context
    Eithers.traverse(tasty.methods)(toUnit)

  private def toUnit(method: LoadedMethod)(using Context): Either[Error, CodeUnit] =
    val position = method.position
    val unit =
      for
        line <- SourceLine.from(position.line)
        name <- UnitName.from(s"${method.owner}.${method.name}")
        cc <- Complexity.from(CyclomaticComplexity.ofUnit(method))
      yield CodeUnit(
        file = SourcePathNormalizer.normalize(position.sourceFile, Some(baseDir)),
        startOffset = position.startOffset,
        endOffset = position.endOffset,
        line = line,
        name = name,
        cc = cc
      )
    unit.left.map(Error.InvalidValue(_))
end TastySource
