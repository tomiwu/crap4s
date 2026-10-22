package crap4s.tasty

import crap4s.CodeUnit
import crap4s.Complexity
import crap4s.Error
import crap4s.SourceLine
import crap4s.UnitName
import crap4s.UnitSource
import crap4s.coverage.SourcePathNormalizer
import tastyquery.Contexts.Context
import tastyquery.Exceptions.InvalidProgramStructureException

import java.nio.file.Path

/** Reads the units from TASTy files. */
final class TastySource(tastyDirs: List[Path], classpath: List[Path], baseDir: Path)
    extends UnitSource:

  def units(): Either[Error, List[CodeUnit]] =
    try TastyLoader.load(tastyDirs, classpath).flatMap(toUnits)
    catch case e: InvalidProgramStructureException => Left(Error.UnresolvedSymbol(e.getMessage))

  private def toUnits(tasty: TastyLoadResult): Either[Error, List[CodeUnit]] =
    given Context = tasty.context
    tasty.methods.foldRight(Right(Nil): Either[Error, List[CodeUnit]]): (method, acc) =>
      for
        rest <- acc
        unit <- toUnit(method)
      yield unit :: rest

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
