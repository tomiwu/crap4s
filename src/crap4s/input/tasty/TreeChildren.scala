package crap4s.input.tasty

import tastyquery.Trees.Tree

/** Returns the direct children of a tree node, or of a list or `Either` that holds tree nodes. */
def treeChildren(node: Any): List[Any] =
  node match
    case tree: Tree => tree.productIterator.toList
    case other => ofContainer(other)

private def ofContainer(node: Any): List[Any] =
  node match
    case either: Either[?, ?] => List(either.fold(identity, identity))
    case items: IterableOnce[?] => items.iterator.toList
    case _ => Nil
