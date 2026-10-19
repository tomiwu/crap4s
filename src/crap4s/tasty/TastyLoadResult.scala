package crap4s.tasty

import tastyquery.Trees.TermTree

enum TopLevelKind:
  case Class, Object

final case class TopLevelDecl(name: String, kind: TopLevelKind)

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
    body: Option[TermTree]
)

final case class TastyLoadResult(
    classpathEntries: List[String],
    topLevels: List[TopLevelDecl],
    methods: List[LoadedMethod]
)
