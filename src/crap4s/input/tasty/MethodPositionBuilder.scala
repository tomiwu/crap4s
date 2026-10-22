package crap4s.input.tasty

import tastyquery.SourcePosition

def positionOf(pos: SourcePosition): MethodPosition =
  MethodPosition(
    sourceFile = pos.sourceFile.path,
    startOffset = pos.startOffset,
    endOffset = pos.endOffset,
    line = pos.startLine
  )
