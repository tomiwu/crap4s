package crap4s.tasty

import tastyquery.Contexts.Context
import tastyquery.Symbols.ClassSymbol
import tastyquery.Symbols.TermOrTypeSymbol

object TopLevelDeclBuilder:

  def fromSymbol(sym: TermOrTypeSymbol)(using Context): Option[TopLevelDecl] =
    sym match
      case cls: ClassSymbol if cls.owner.isPackage => Some(fromClass(cls))
      case _                                       => None

  def topLevelName(cls: ClassSymbol): String =
    val full = cls.displayFullName
    if cls.isModuleClass then full.stripSuffix("$") else full

  private def fromClass(cls: ClassSymbol): TopLevelDecl =
    TopLevelDecl(topLevelName(cls), kindOf(cls))

  private def kindOf(cls: ClassSymbol): TopLevelKind =
    if cls.isModuleClass then TopLevelKind.Object else TopLevelKind.Class
