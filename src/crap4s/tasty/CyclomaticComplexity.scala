package crap4s.tasty

import tastyquery.Contexts.Context
import tastyquery.Names.SignedName
import tastyquery.Names.TermName
import tastyquery.Trees.*

object CyclomaticComplexity:

  private val BooleanOperators: Set[String] = Set("&&", "||")

  def of(body: Option[TermTree])(using Context): Int =
    1 + count(body, inUnit = false)

  def ofUnit(unit: LoadedMethod)(using Context): Int =
    1 + count(unit.body, inUnit = unit.kind == UnitKind.Lambda)

  def isSynthetic(defDef: DefDef): Boolean =
    defDef.symbol.isSynthetic && !defDef.name.toString.startsWith("$anonfun")

  private def count(node: Any, inUnit: Boolean)(using Context): Int =
    node match
      case _: Inlined => 0
      case defDef: DefDef => countDef(defDef, inUnit)
      case other => own(other) + sum(TreeChildren.of(other), inUnit)

  private def countDef(defDef: DefDef, inUnit: Boolean)(using Context): Int =
    if isSynthetic(defDef) || !inUnit then 0 else sum(TreeChildren.of(defDef), inUnit)

  private def sum(nodes: List[Any], inUnit: Boolean)(using Context): Int =
    nodes.map(count(_, inUnit)).sum

  private def own(node: Any)(using Context): Int =
    node match
      case tree: Tree => ownTree(tree)
      case _ => 0

  private def ownTree(tree: Tree)(using Context): Int =
    tree match
      case _: If | _: InlineIf | _: While => 1
      case select: Select => selectCount(select)
      case other => caseCount(other)

  private def caseCount(tree: Tree): Int =
    tree match
      case caseDef: CaseDef => caseDef.guard.size
      case other => extraCases(switchCases(other))

  private def switchCases(tree: Tree): List[CaseDef] =
    tree match
      case matchTree: Match => matchTree.cases
      case matchTree: InlineMatch => matchTree.cases
      case other => tryCases(other)

  private def tryCases(tree: Tree): List[CaseDef] =
    tree match
      case tryTree: Try => tryTree.cases
      case _ => Nil

  private def selectCount(select: Select)(using Context): Int =
    val name = plainName(select.name)
    if name == "withFilter" || isBooleanOperator(select, name) then 1 else 0

  private def isBooleanOperator(select: Select, name: String)(using Context): Boolean =
    BooleanOperators.contains(name) && select.symbol.owner.name.toString == "Boolean" &&
      select.symbol.owner.owner.name.toString == "scala"

  private def extraCases(cases: List[CaseDef]): Int = (cases.size - 1).max(0)

  private def plainName(name: TermName): String =
    name match
      case signed: SignedName => signed.underlying.toString
      case other => other.toString
end CyclomaticComplexity
