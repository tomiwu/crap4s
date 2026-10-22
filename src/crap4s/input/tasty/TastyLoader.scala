package crap4s.input.tasty

import crap4s.core.Error
import tastyquery.Contexts.Context
import tastyquery.Symbols.ClassSymbol
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.jdk.ClasspathLoaders

import java.nio.file.Path

def loadTasty(tastyDir: Path): Either[Error, TastyLoadResult] =
  loadTasty(List(tastyDir), Nil)

def loadTasty(tastyDirs: List[Path], extra: List[Path]): Either[Error, TastyLoadResult] =
  val labels = entryLabels(tastyDirs)
  val classpath = ClasspathLoaders.read(classpathPaths(tastyDirs, extra))
  val ctx = tastyquery.Contexts.Context.initialize(classpath)
  given Context = ctx
  val symbols = classpath.take(
    tastyDirs.map(normalizedTastyDir).distinct.size
  ).flatMap(ctx.findSymbolsByClasspathEntry)
  val topLevels = symbols.flatMap(topLevelDecl)
  val methods = symbols.flatMap(methodsFromTopLevel)
  Right(TastyLoadResult(labels, topLevels, methods, ctx))

private def methodsFromTopLevel(sym: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
  sym match
    case cls: ClassSymbol if cls.owner.isPackage =>
      val owner = topLevelName(cls)
      cls.declarations.flatMap(methodsFromOwner(owner, _))
    case _ => Nil
