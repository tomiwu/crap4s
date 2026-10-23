package crap4s.input.tasty

import crap4s.core.Error
import crap4s.core.attempt
import crap4s.core.describe
import tastyquery.Contexts.Context
import tastyquery.Symbols.ClassSymbol
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.Exceptions.InvalidProgramStructureException
import tastyquery.jdk.ClasspathLoaders

import java.nio.file.Path

def loadTasty(tastyDir: Path): Either[Error, TastyLoadResult] =
  loadTasty(List(tastyDir), Nil)

def loadTasty(tastyDirs: List[Path], extra: List[Path]): Either[Error, TastyLoadResult] =
  classpathPaths(tastyDirs, extra).flatMap: paths =>
    attempt(readTasty(tastyDirs, paths))(tastyError)

/** Maps an exception of tasty-query to a typed error. */
def tastyError(e: Throwable): Error = e match
  case _: InvalidProgramStructureException => Error.UnresolvedSymbol(describe(e))
  case _ => Error.TastyLoadFailed(describe(e))

private def readTasty(tastyDirs: List[Path], paths: List[Path]): TastyLoadResult =
  val classpath = ClasspathLoaders.read(paths)
  val ctx = tastyquery.Contexts.Context.initialize(classpath)
  given Context = ctx
  val symbols = classpath.take(
    tastyDirs.map(normalizedTastyDir).distinct.size
  ).flatMap(ctx.findSymbolsByClasspathEntry)
  val topLevels = symbols.flatMap(topLevelDecl)
  val methods = symbols.flatMap(methodsFromTopLevel)
  TastyLoadResult(entryLabels(tastyDirs), topLevels, methods, ctx)

private def methodsFromTopLevel(sym: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
  sym match
    case cls: ClassSymbol if cls.owner.isPackage =>
      val owner = topLevelName(cls)
      cls.declarations.flatMap(methodsFromOwner(owner, _))
    case _ => Nil
