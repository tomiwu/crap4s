package crap4s.input.tasty

import tastyquery.Contexts.Context
import tastyquery.Names.nme
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.Symbols.TermSymbol
import tastyquery.Trees.DefDef

object LoadedMethodBuilder:

  def fromOwner(owner: String, term: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
    for
      method <- userMethod(term).toList
      defTree <- method.tree.collect { case defDef: DefDef => defDef }.toList
      unit <- fromDefTree(owner, method.name.toString, defTree)
    yield unit

  private def userMethod(term: TermOrTypeSymbol): Option[TermSymbol] =
    term match
      case method: TermSymbol if isUserMethod(method) => Some(method)
      case _ => None

  private def isUserMethod(method: TermSymbol): Boolean =
    method.isMethod && method.name != nme.Constructor && !method.isSynthetic

  private def fromDefTree(owner: String, name: String, tree: DefDef)(using
      Context
  ): List[LoadedMethod] =
    val method = LoadedMethod(
      owner = owner,
      name = name,
      position = MethodPositionBuilder.fromSourcePosition(tree.pos),
      body = tree.rhs
    )
    method :: LambdaUnits.of(method)
end LoadedMethodBuilder
