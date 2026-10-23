import scala.meta._
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/**
 * Flags a call that can throw: get without arguments (Option, Try), head, last, tail, reduce, toInt,
 * toDouble, require, assert and ???. Test sources are allowed.
 */
class NoUnsafeCall extends SyntacticRule("NoUnsafeCall") {
  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(NoUnsafeCall.findings(doc.tree, doc.input.syntax).map { f =>
      Patch.lint(Diagnostic("NoUnsafeCall", f.message, f.position, "", LintSeverity.Error))
    })
}

object NoUnsafeCall {
  final case class Finding(message: String, position: Position)

  private val members = Set("head", "last", "tail", "reduce", "toInt", "toDouble")
  private val functions = Set("require", "assert")

  def findings(tree: Tree, path: String): List[Finding] =
    if (!inSource(path)) Nil
    else
      tree.collect {
        case s: Term.Select if unsafeMember(s) => finding(s.name.value, s.name)
        case n @ Term.Name("???") => finding("???", n)
        case a: Term.Apply if unsafeFunction(a) => finding(fnName(a), a.fun)
      }

  private def finding(name: String, at: Tree): Finding =
    Finding(s"NoUnsafeCall: $name can throw; return a value in an Either or an Option", at.pos)

  private def unsafeMember(s: Term.Select): Boolean =
    members(s.name.value) || (s.name.value == "get" && !applied(s))

  private def applied(s: Term.Select): Boolean = s.parent.exists {
    case Term.Apply.After_4_6_0(fn, _) => fn eq s
    case Term.ApplyType.After_4_6_0(fn, _) => fn eq s
    case _ => false
  }

  private def unsafeFunction(a: Term.Apply): Boolean = a.fun match {
    case Term.Name(n) => functions(n)
    case _ => false
  }

  private def fnName(a: Term.Apply): String = a.fun.syntax

  private def inSource(path: String): Boolean = {
    val p = path.replace('\\', '/')
    (p.startsWith("src/") || p.contains("/src/")) && !p.endsWith(".test.scala")
  }
}
