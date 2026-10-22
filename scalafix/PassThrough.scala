import scala.meta._
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/** Flags a def whose body only calls another function with its own parameters or constants. */
class PassThrough extends SyntacticRule("PassThrough") {
  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(PassThrough.findings(doc.tree).map { f =>
      Patch.lint(Diagnostic("PassThrough", f.message, f.position, "", LintSeverity.Error))
    })
}

object PassThrough {
  final case class Finding(message: String, position: Position)

  private val suppression = "crap4s:passthrough"

  def findings(tree: Tree): List[Finding] =
    tree.collect {
      case d: Defn.Def if !excluded(d) => callee(d).map(c => finding(d, c))
    }.flatten

  private def finding(d: Defn.Def, callee: Term): Finding =
    Finding(s"PassThrough: ${d.name.value} only calls ${callee.syntax}; inline it", d.name.pos)

  private def callee(d: Defn.Def): Option[Term] =
    call(stripAscriptions(d.body)).filter { case (fn, args) =>
      isPlainPath(fn) && !isSelf(fn, d) && args.forall(isPassedArg(_, params(d)))
    }.map(_._1)

  private def isSelf(fn: Term, d: Defn.Def): Boolean = fn match {
    case Term.Name(n) => n == d.name.value
    case _ => false
  }

  private def params(d: Defn.Def): Set[String] =
    d.paramClauseGroups.flatMap(_.paramClauses).flatMap(_.values).map(_.name.value).toSet ++
      d.paramClauses.flatMap(_.values).map(_.name.value)

  @scala.annotation.tailrec
  private def stripAscriptions(t: Term): Term = t match {
    case Term.Ascribe(e, _) => stripAscriptions(e)
    case Term.Block(List(single: Term)) => stripAscriptions(single)
    case other => other
  }

  private def call(t: Term): Option[(Term, List[Term])] = t match {
    case Term.Apply.After_4_6_0(fn, clause) =>
      val inner = fn match {
        case Term.ApplyType.After_4_6_0(f, _) => f
        case f => f
      }
      call(inner) match {
        case Some((f, earlier)) => Some((f, earlier ++ clause.values))
        case None => Some((inner, clause.values))
      }
    case _ => None
  }

  private def isPlainPath(t: Term): Boolean = t match {
    case _: Term.Name | _: Term.This | _: Term.Super => true
    case Term.Select(q, _) => isPlainPath(q)
    case _ => false
  }

  private def isPassedArg(arg: Term, own: Set[String]): Boolean = stripAscriptions(arg) match {
    case Term.Name(n) => own.contains(n)
    case _: Lit => true
    case Term.Assign(_, v) => isPassedArg(v, own)
    case _ => false
  }

  private def excluded(d: Defn.Def): Boolean =
    d.mods.exists(_.isInstanceOf[Mod.Override]) ||
      suppressed(d) ||
      inheriting(d) ||
      companionApply(d)

  private def suppressed(d: Defn.Def): Boolean = d.mods.exists {
    case Mod.Annot(Init.After_4_6_0(Type.Name("nowarn"), _, clauses)) =>
      clauses.exists(_.values.exists { case Lit.String(s) => s == suppression; case _ => false })
    case _ => false
  }

  private def owner(t: Tree): Option[Tree] = t.parent.flatMap {
    case _: Template.Body => t.parent.flatMap(_.parent)
    case other => Some(other)
  }

  private def inheriting(d: Defn.Def): Boolean = owner(d) match {
    case Some(t: Template) => t.inits.nonEmpty
    case _ => false
  }

  private def companionApply(d: Defn.Def): Boolean =
    d.name.value == "apply" && (owner(d).flatMap(owner) match {
      case Some(o: Defn.Object) => hasSiblingType(o)
      case _ => false
    })

  private def hasSiblingType(o: Defn.Object): Boolean =
    o.parent.toList.flatMap(_.children).exists {
      case c: Defn.Class => c.name.value == o.name.value
      case t: Defn.Trait => t.name.value == o.name.value
      case e: Defn.Enum => e.name.value == o.name.value
      case _ => false
    }
}
