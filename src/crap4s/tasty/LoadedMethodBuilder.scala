package crap4s.tasty

import tastyquery.Contexts.Context
import tastyquery.Names.nme
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.Symbols.TermSymbol
import tastyquery.Trees.DefDef

object LoadedMethodBuilder:

  def fromOwner(owner: String, term: TermOrTypeSymbol)(using Context): List[LoadedMethod] =
    term match
      case method: TermSymbol if method.isMethod && isUserMethod(method) =>
        method.tree match
          case Some(defTree: DefDef) => fromDefTree(owner, method.name.toString, defTree)
          case _                     => Nil
      case _ => Nil

  private def isUserMethod(method: TermSymbol): Boolean =
    method.name != nme.Constructor

  private def fromDefTree(owner: String, name: String, tree: DefDef): List[LoadedMethod] =
    List(
      LoadedMethod(
        owner = owner,
        name = name,
        position = MethodPositionBuilder.fromSourcePosition(tree.pos),
        body = tree.rhs
      )
    )
