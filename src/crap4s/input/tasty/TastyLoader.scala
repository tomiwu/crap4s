package crap4s.input.tasty

import crap4s.core.Error
import tastyquery.Contexts.Context
import tastyquery.Symbols.ClassSymbol
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.jdk.ClasspathLoaders

import java.nio.file.Path

object TastyLoader:

  def load(tastyDir: Path): Either[Error, TastyLoadResult] =
    load(List(tastyDir), Nil)

  def load(tastyDirs: List[Path], extra: List[Path]): Either[Error, TastyLoadResult] =
    val labels = TastyClasspath.entryLabels(tastyDirs)
    val classpath = ClasspathLoaders.read(TastyClasspath.paths(tastyDirs, extra))
    val ctx = tastyquery.Contexts.Context.initialize(classpath)
    given Context = ctx
    val symbols = classpath.take(
      tastyDirs.map(TastyClasspath.normalizedTastyDir).distinct.size
    ).flatMap(ctx.findSymbolsByClasspathEntry)
    val topLevels = symbols.flatMap(TopLevelDeclBuilder.fromSymbol)
    val methods = symbols.flatMap(methodsFromTopLevel)
    Right(TastyLoadResult(labels, topLevels, methods, ctx))

  private def methodsFromTopLevel(sym: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
    sym match
      case cls: ClassSymbol if cls.owner.isPackage =>
        val owner = TopLevelDeclBuilder.topLevelName(cls)
        cls.declarations.flatMap(LoadedMethodBuilder.fromOwner(owner, _))
      case _ => Nil
end TastyLoader
