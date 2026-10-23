import scala.meta._
import metaconfig.Configured
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/**
 * Flags a try outside the edge files. Flags a catch that does not return a Left, except in the
 * guard files. The files are path parts in `UncheckedTryFiles` and `UncheckedTryGuards`.
 */
class NoUncheckedTry(edge: List[String], guards: List[String])
    extends SyntacticRule("NoUncheckedTry") {
  def this() = this(Nil, Nil)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("UncheckedTryFiles")(edge)
      .product(config.conf.getOrElse("UncheckedTryGuards")(guards))
      .map { case (e, g) => new NoUncheckedTry(e, g) }

  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(NoUncheckedTry.findings(doc.tree, doc.input.syntax, edge, guards).map { f =>
      Patch.lint(Diagnostic("NoUncheckedTry", f.message, f.position, "", LintSeverity.Error))
    })
}

object NoUncheckedTry {
  final case class Finding(message: String, position: Position)

  def findings(tree: Tree, path: String, edge: List[String], guards: List[String]): List[Finding] =
    if (!inSource(path)) Nil
    else
      tree.collect {
        case t: Term.Try => tryFindings(t, listed(path, edge), listed(path, guards))
        case t: Term.TryWithHandler => outside(t, listed(path, edge))
      }.flatten

  private def tryFindings(t: Term.Try, edge: Boolean, guard: Boolean): List[Finding] =
    outside(t, edge) ++ (if (guard) Nil else t.catchp.filterNot(c => returnsLeft(c.body)).map(leftFinding))

  private def outside(t: Tree, edge: Boolean): List[Finding] =
    if (edge) Nil
    else List(Finding("NoUncheckedTry: do not use try outside the edge files", t.pos))

  private def leftFinding(c: Case): Finding =
    Finding("NoUncheckedTry: a catch must return Left(...)", c.pos)

  @scala.annotation.tailrec
  private def returnsLeft(t: Term): Boolean = t match {
    case Term.Block(List(single: Term)) => returnsLeft(single)
    case Term.Apply.After_4_6_0(Term.Name("Left"), _) => true
    case Term.Apply.After_4_6_0(Term.ApplyType.After_4_6_0(Term.Name("Left"), _), _) => true
    case _ => false
  }

  private def listed(path: String, parts: List[String]): Boolean =
    parts.exists(path.replace('\\', '/').contains)

  private def inSource(path: String): Boolean = {
    val p = path.replace('\\', '/')
    (p.startsWith("src/") || p.contains("/src/")) && !p.endsWith(".test.scala")
  }
}
