package crap4s.input.tasty

import tastyquery.Contexts.Context
import tastyquery.Trees.TermTree

enum TopLevelKind:
  case Class, Object

final case class TopLevelDecl(name: String, kind: TopLevelKind)

enum UnitKind:
  case Method, Lambda

final case class MethodPosition(
    sourceFile: String,
    startOffset: Int,
    endOffset: Int,
    line: Int
)

final case class LoadedMethod(
    owner: String,
    name: String,
    position: MethodPosition,
    body: Option[TermTree],
    kind: UnitKind = UnitKind.Method
)

final case class TastyLoadResult(
    classpathEntries: List[String],
    topLevels: List[TopLevelDecl],
    methods: List[LoadedMethod],
    context: Context
)
