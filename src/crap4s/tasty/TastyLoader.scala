package crap4s.tasty

import tastyquery.Contexts.Context
import tastyquery.Symbols.ClassSymbol
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.jdk.ClasspathLoaders

import java.nio.file.Path

object TastyLoader:

  def load(tastyDir: Path): Either[String, TastyLoadResult] =
    val labels = TastyClasspath.entryLabels(tastyDir)
    val classpath = ClasspathLoaders.read(TastyClasspath.paths(tastyDir))
    val ctx = tastyquery.Contexts.Context.initialize(classpath)
    given Context = ctx
    val symbols = ctx.findSymbolsByClasspathEntry(classpath.head).toList
    val topLevels = symbols.flatMap(TopLevelDeclBuilder.fromSymbol)
    val methods = symbols.flatMap(methodsFromTopLevel)
    Right(TastyLoadResult(labels, topLevels, methods, ctx))

  private def methodsFromTopLevel(sym: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
    sym match
      case cls: ClassSymbol if cls.owner.isPackage =>
        val owner = TopLevelDeclBuilder.topLevelName(cls)
        cls.declarations.flatMap(LoadedMethodBuilder.fromOwner(owner, _))
      case _ => Nil
