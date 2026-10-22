import scala.meta._
import metaconfig.Configured
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/** Flags a class, object or enum case that extends a capability. */
class NoExtendsCapability(configured: List[String]) extends SyntacticRule("NoExtendsCapability") {
  def this() = this(Nil)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("CapabilityStyle")(configured)
      .map(names => new NoExtendsCapability(names))

  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(NoExtendsCapability.findings(doc.tree, configured.toSet).map { f =>
      Patch.lint(Diagnostic("NoExtendsCapability", f.message, f.position, "", LintSeverity.Error))
    })
}

object NoExtendsCapability {
  final case class Finding(message: String, position: Position)

  def findings(tree: Tree, names: Set[String]): List[Finding] =
    inits(tree).flatMap(i => baseName(i.tpe).filter(names).map(n => finding(n, i)))

  private def finding(name: String, init: Init): Finding =
    Finding(s"NoExtendsCapability: do not extend the capability $name; use it with ?=>", init.pos)

  private def inits(tree: Tree): List[Init] =
    tree.collect {
      case t: Template => t.inits
      case e: Defn.EnumCase => e.inits
    }.flatten

  private def baseName(t: Type): Option[String] = t match {
    case Type.Name(n) => Some(n)
    case Type.Select(_, Type.Name(n)) => Some(n)
    case Type.Apply.After_4_6_0(f, _) => baseName(f)
    case _ => None
  }
}
