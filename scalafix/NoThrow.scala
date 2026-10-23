import scala.meta._
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/** Flags a throw in a source file. Return a Left instead. Test sources are allowed. */
class NoThrow extends SyntacticRule("NoThrow") {
  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(NoThrow.findings(doc.tree, doc.input.syntax).map { f =>
      Patch.lint(Diagnostic("NoThrow", f.message, f.position, "", LintSeverity.Error))
    })
}

object NoThrow {
  final case class Finding(message: String, position: Position)

  def findings(tree: Tree, path: String): List[Finding] =
    if (!inSource(path)) Nil
    else
      tree.collect { case t: Term.Throw =>
        Finding("NoThrow: do not throw; return a Left(...) with a typed error", t.pos)
      }

  private def inSource(path: String): Boolean = {
    val p = path.replace('\\', '/')
    (p.startsWith("src/") || p.contains("/src/")) && !p.endsWith(".test.scala")
  }
}
