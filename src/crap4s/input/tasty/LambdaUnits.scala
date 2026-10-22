package crap4s.input.tasty

import tastyquery.Contexts.Context
import tastyquery.Trees.*

private val LambdaPrefix: String = "$anonfun"

def lambdaUnits(parent: LoadedMethod)(using Context): List[LoadedMethod] =
  topLevelDefs(parent.body)
    .map(lambdaToUnit(parent, _))
    .filter(unit => complexityOfUnit(unit) > 1)

def lambdaUnitName(parentName: String, defName: String, line: Int): String =
  val label = if defName.startsWith(LambdaPrefix) then "lambda" else defName
  s"$parentName$$$label@$line"

private def lambdaToUnit(parent: LoadedMethod, defDef: DefDef): LoadedMethod =
  val position = positionOf(defDef.pos)
  LoadedMethod(
    owner = parent.owner,
    name = lambdaUnitName(parent.name, defDef.name.toString, position.line),
    position = position,
    body = defDef.rhs,
    kind = UnitKind.Lambda
  )

private def topLevelDefs(node: Any)(using Context): List[DefDef] =
  node match
    case _: Inlined => Nil
    case defDef: DefDef => userDef(defDef)
    case other => treeChildren(other).flatMap(topLevelDefs)

private def userDef(defDef: DefDef): List[DefDef] =
  if isSyntheticDef(defDef) then Nil else List(defDef)
