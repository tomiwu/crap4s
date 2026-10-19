package crap4s.tasty

import java.nio.file.Path

import tastyquery.Classpaths.ClasspathEntry
import tastyquery.Contexts.Context
import tastyquery.Symbols.ClassSymbol
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.jdk.ClasspathLoaders

object TastyLoader:

  def load(tastyDir: Path): Either[String, TastyLoadResult] =
    val labels = TastyClasspath.entryLabels(tastyDir)
    val classpath = ClasspathLoaders.read(TastyClasspath.paths(tastyDir))
    val ctx = tastyquery.Contexts.Context.initialize(classpath)
    loadWithContext(classpath, labels, ctx)

  private def loadWithContext(
      classpath: List[ClasspathEntry],
      labels: List[String],
      ctx: Context
  ): Either[String, TastyLoadResult] =
    given Context = ctx
    val tastyEntry = classpath.head
    val symbols = ctx.findSymbolsByClasspathEntry(tastyEntry).toList
    val topLevels = symbols.flatMap(TopLevelDeclBuilder.fromSymbol)
    val methods = symbols.flatMap(collectMethods)
    Right(TastyLoadResult(labels, topLevels, methods))

  private def collectMethods(sym: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
    sym match
      case cls: ClassSymbol if cls.owner.isPackage =>
        val owner = TopLevelDeclBuilder.fromSymbol(cls).map(_.name).getOrElse(cls.displayFullName)
        cls.declarations.flatMap(LoadedMethodBuilder.fromOwner(owner, _))
      case _ => Nil
