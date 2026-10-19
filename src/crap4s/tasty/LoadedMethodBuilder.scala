package crap4s.tasty

import tastyquery.Contexts.Context
import tastyquery.Names.nme
import tastyquery.Symbols.TermOrTypeSymbol
import tastyquery.Symbols.TermSymbol
import tastyquery.Trees.DefDef

object LoadedMethodBuilder:

  def fromOwner(owner: String, term: TermOrTypeSymbol)(using Context): Option[LoadedMethod] =
    term match
      case method: TermSymbol if method.isMethod && isUserMethod(method) =>
        method.tree.collect {
          case defTree: DefDef =>
            LoadedMethod(
              owner = owner,
              name = method.name.toString,
              position = MethodPositionBuilder.fromSourcePosition(defTree.pos),
              body = defTree.rhs
            )
        }
      case _ => None

  private def isUserMethod(method: TermSymbol): Boolean =
    method.name != nme.Constructor
