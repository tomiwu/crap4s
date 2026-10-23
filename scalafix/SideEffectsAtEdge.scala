import scala.meta._
import metaconfig.Configured
import scalafix.lint.Diagnostic
import scalafix.lint.LintSeverity
import scalafix.v1._

/**
 * Flags a side effect outside the edge files: exit, print, Console, System streams and
 * environment, java.io, Files, FileSystems, var and scala.collection.mutable. The edge files are
 * path parts in `SideEffectsAllow`. Test sources are allowed.
 */
class SideEffectsAtEdge(allow: List[String]) extends SyntacticRule("SideEffectsAtEdge") {
  def this() = this(Nil)

  override def withConfiguration(config: Configuration): Configured[Rule] =
    config.conf
      .getOrElse("SideEffectsAllow")(allow)
      .map(parts => new SideEffectsAtEdge(parts))

  override def fix(implicit doc: SyntacticDocument): Patch =
    Patch.fromIterable(SideEffectsAtEdge.findings(doc.tree, doc.input.syntax, allow).map { f =>
      Patch.lint(Diagnostic("SideEffectsAtEdge", f.message, f.position, "", LintSeverity.Error))
    })
}

object SideEffectsAtEdge {
  final case class Finding(message: String, position: Position)

  private val systemMembers = Set("exit", "getenv", "getProperty", "console", "out", "err", "in")
  private val printers = Set("print", "println")
  private val fileClasses = Set("Files", "FileSystems")

  def findings(tree: Tree, path: String, allow: List[String]): List[Finding] =
    if (!inSource(path) || listed(path, allow)) Nil
    else tree.collect(effect).flatten

  private def effect: PartialFunction[Tree, Option[Finding]] = {
    case s: Term.Select if !inImport(s) => selectEffect(s).map(finding(_, s))
    case Term.Apply.After_4_6_0(n @ Term.Name(p), _) if printers(p) => Some(finding(p, n))
    case n @ Term.Name("Console") => Some(finding("Console", n))
    case v: Defn.Var => Some(finding("var", v))
    case i: Importer => importEffect(i).map(finding(_, i))
  }

  private def finding(what: String, at: Tree): Finding =
    Finding(s"SideEffectsAtEdge: $what is a side effect; keep it in the edge files", at.pos)

  private def selectEffect(s: Term.Select): Option[String] =
    s.qual match {
      case Term.Name("sys") if s.name.value == "exit" => Some("sys.exit")
      case q if isSystem(q) && systemMembers(s.name.value) => Some(s"System.${s.name.value}")
      case _ if !innerOfSelect(s) => qualified(s)
      case _ => None
    }

  @scala.annotation.tailrec
  private def inImport(t: Tree): Boolean = t.parent match {
    case Some(_: Importer) => true
    case Some(p: Term.Select) => inImport(p)
    case _ => false
  }

  private def innerOfSelect(s: Term.Select): Boolean = s.parent.exists {
    case outer: Term.Select => outer.qual eq s
    case _ => false
  }

  private def isSystem(t: Term): Boolean = t.syntax == "System" || t.syntax == "java.lang.System"

  private def qualified(s: Term.Select): Option[String] = {
    val text = s.syntax
    if (text.startsWith("java.io.")) Some("java.io")
    else if (fileClasses.exists(c => text.startsWith(s"java.nio.file.$c"))) Some(text)
    else if (text.startsWith("scala.collection.mutable") || text.startsWith("mutable."))
      Some("scala.collection.mutable")
    else None
  }

  private def importEffect(i: Importer): Option[String] = {
    val ref = i.ref.syntax
    if (ref == "java.io") Some("java.io")
    else if (ref == "java.nio.file" && i.importees.exists(fileImportee)) Some("java.nio.file.Files")
    else if (ref == "scala.collection.mutable" || ref == "collection.mutable")
      Some("scala.collection.mutable")
    else if (ref == "scala.collection" && i.importees.exists(_.syntax == "mutable"))
      Some("scala.collection.mutable")
    else None
  }

  private def fileImportee(i: Importee): Boolean = i match {
    case Importee.Name(Name(n)) => fileClasses(n)
    case Importee.Rename(Name(n), _) => fileClasses(n)
    case _: Importee.Wildcard => true
    case _ => false
  }

  private def listed(path: String, parts: List[String]): Boolean =
    parts.exists(path.replace('\\', '/').contains)

  private def inSource(path: String): Boolean = {
    val p = path.replace('\\', '/')
    (p.startsWith("src/") || p.contains("/src/")) && !p.endsWith(".test.scala")
  }
}
