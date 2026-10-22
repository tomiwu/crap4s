import scala.meta._
import metaconfig.Configured
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/** Flags a given of a capability outside App.scala. Test sources are allowed. */
class GivenOnlyInApp(configured: List[String]) extends SyntacticRule("GivenOnlyInApp") {
  def this() = this(Nil)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("CapabilityStyle")(configured)
      .map(names => new GivenOnlyInApp(names))

  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(
      GivenOnlyInApp.findings(doc.tree, configured.toSet, doc.input.syntax).map { f =>
        Patch.lint(Diagnostic("GivenOnlyInApp", f.message, f.position, "", LintSeverity.Error))
      }
    )
}

object GivenOnlyInApp {
  final case class Finding(message: String, position: Position)

  def findings(tree: Tree, names: Set[String], path: String): List[Finding] =
    if (exempt(path)) Nil
    else
      tree.collect {
        case g: Defn.GivenAlias => typeName(g.decltpe).filter(names).map(n => finding(n, g))
        case g: Defn.Given =>
          g.templ.inits.headOption.flatMap(i => typeName(i.tpe)).filter(names).map(n => finding(n, g))
      }.flatten

  private def finding(name: String, g: Tree): Finding =
    Finding(s"GivenOnlyInApp: give the capability $name only in App.scala", g.pos)

  private def exempt(path: String): Boolean = {
    val p = path.replace('\\', '/')
    p.endsWith("/App.scala") || p == "App.scala" || p.endsWith(".test.scala") ||
    p.startsWith("test/") || p.contains("/test/")
  }

  private def typeName(t: Type): Option[String] = t match {
    case Type.Name(n) => Some(n)
    case Type.Select(_, Type.Name(n)) => Some(n)
    case _ => None
  }
}
