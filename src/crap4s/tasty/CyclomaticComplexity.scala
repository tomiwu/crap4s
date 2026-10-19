package crap4s.tasty

import tastyquery.Names.SignedName
import tastyquery.Names.TermName
import tastyquery.Trees.*

object CyclomaticComplexity:

  private val BranchingOperators: Set[String] = Set("&&", "||", "withFilter")

  def of(body: Option[TermTree]): Int =
    1 + count(body)

  private def count(node: Any): Int =
    node match
      case _: Inlined                       => 0
      case defDef: DefDef if isSynthetic(defDef) => 0
      case tree: Tree                       => own(tree) + sum(tree.productIterator)
      case items: IterableOnce[?]           => sum(items.iterator)
      case Left(value)                      => count(value)
      case Right(value)                     => count(value)
      case _                                => 0

  private def sum(nodes: Iterator[Any]): Int =
    nodes.map(count).sum

  private def isSynthetic(defDef: DefDef): Boolean =
    defDef.symbol.isSynthetic && !defDef.name.toString.startsWith("$anonfun")

  private def own(tree: Tree): Int =
    tree match
      case _: If | _: InlineIf | _: While => 1
      case matchTree: Match               => extraCases(matchTree.cases)
      case matchTree: InlineMatch         => extraCases(matchTree.cases)
      case tryTree: Try                   => extraCases(tryTree.cases)
      case caseDef: CaseDef               => caseDef.guard.size
      case select: Select                 => if BranchingOperators.contains(plainName(select.name)) then 1 else 0
      case _                              => 0

  private def extraCases(cases: List[CaseDef]): Int =
    (cases.size - 1).max(0)

  private def plainName(name: TermName): String =
    name match
      case signed: SignedName => signed.underlying.toString
      case other              => other.toString
