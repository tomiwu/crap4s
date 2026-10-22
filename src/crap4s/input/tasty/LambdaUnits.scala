package crap4s.input.tasty

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
      case _: Inlined => Nil
      case defDef: DefDef => userDef(defDef)
      case other => TreeChildren.of(other).flatMap(topLevelDefs)

  private def userDef(defDef: DefDef): List[DefDef] =
    if CyclomaticComplexity.isSynthetic(defDef) then Nil else List(defDef)
end LambdaUnits
