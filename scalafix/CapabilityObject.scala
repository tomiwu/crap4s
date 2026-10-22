import scala.meta._
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/** Flags an object that holds only defs. Opaque-type companions and App are allowed. */
class CapabilityObject extends SyntacticRule("CapabilityObject") {
  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(CapabilityObject.findings(doc.tree).map { f =>
      Patch.lint(Diagnostic("CapabilityObject", f.message, f.position, "", LintSeverity.Error))
    })
}

object CapabilityObject {
  final case class Finding(message: String, position: Position)

  def findings(tree: Tree): List[Finding] =
    tree.collect {
      case o: Defn.Object if holdsOnlyDefs(o) && !allowed(o) =>
        Finding(
          s"CapabilityObject: ${o.name.value} holds only functions; use top-level defs",
          o.name.pos
        )
    }

  private def holdsOnlyDefs(o: Defn.Object): Boolean =
    o.templ.stats.nonEmpty && o.templ.stats.forall(_.isInstanceOf[Defn.Def])

  private def allowed(o: Defn.Object): Boolean = o.name.value == "App" || opaqueCompanion(o)

  private def opaqueCompanion(o: Defn.Object): Boolean =
    o.parent.toList.flatMap(_.children).exists {
      case t: Defn.Type => t.name.value == o.name.value && t.mods.exists(_.isInstanceOf[Mod.Opaque])
      case _ => false
    }
}
