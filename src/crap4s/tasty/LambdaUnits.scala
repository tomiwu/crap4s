package crap4s.tasty

import tastyquery.Contexts.Context
import tastyquery.Trees.*

object LambdaUnits:

  private val LambdaPrefix: String = "$anonfun"

  def of(parent: LoadedMethod)(using Context): List[LoadedMethod] =
    topLevelDefs(parent.body)
      .map(toUnit(parent, _))
      .filter(unit => CyclomaticComplexity.ofUnit(unit) > 1)

  def unitName(parentName: String, defName: String, line: Int): String =
    val label = if defName.startsWith(LambdaPrefix) then "lambda" else defName
    s"$parentName$$$label@$line"

  private def toUnit(parent: LoadedMethod, defDef: DefDef): LoadedMethod =
    val position = MethodPositionBuilder.fromSourcePosition(defDef.pos)
    LoadedMethod(
      owner = parent.owner,
      name = unitName(parent.name, defDef.name.toString, position.line),
      position = position,
      body = defDef.rhs,
      kind = UnitKind.Lambda
    )

  private def topLevelDefs(node: Any)(using Context): List[DefDef] =
    node match
      case _: Inlined                            => Nil
      case defDef: DefDef if CyclomaticComplexity.isSynthetic(defDef) => Nil
      case defDef: DefDef                        => List(defDef)
      case tree: Tree                            => tree.productIterator.toList.flatMap(topLevelDefs)
      case items: IterableOnce[?]                => items.iterator.toList.flatMap(topLevelDefs)
      case Left(value)                           => topLevelDefs(value)
      case Right(value)                          => topLevelDefs(value)
      case _                                     => Nil
