package crap4s

import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class ClasspathTest extends FunSuite:

  private val base = Path.of("").toAbsolutePath
  private val fixtures = Path.of("test/fixtures").toAbsolutePath.normalize

  private case class Compiled(classes: Path, coverage: Path)

  private def compile(name: String, jars: Seq[Path]): Compiled =
    val classes = Files.createTempDirectory(s"crap4s-$name-classes")
    val coverage = Files.createTempDirectory(s"crap4s-$name-coverage")
    val jarOptions = jars.flatMap(jar => Seq("--jar", jar.toString))
    val cmd =
      Seq("scala-cli", "compile", fixtures.resolve(name).toString, "-d", classes.toString) ++
        jarOptions ++ Seq("-O", s"-coverage-out:$coverage", "-O", s"-sourceroot:$fixtures")
    assertEquals(Process(cmd).!, 0, clue = s"scala-cli compile failed for $name")
    Compiled(classes, coverage)

  private lazy val lib: Compiled = compile("ext-lib", Nil)
  private lazy val app: Compiled = compile("ext-app", Seq(lib.classes))

  private def appArgs(extra: String*): List[String] =
    List("--tasty", app.classes.toString, "--coverage", app.coverage.toString, "--all") ++ extra

  private def units(outcome: Outcome): List[String] = outcome.out.linesIterator.toList.init

  test("an app without its dependencies gives an error that tells to add the classpath"):
    val outcome = Cli.run(appArgs(), base)
    assertEquals(outcome.code, Cli.ErrorCode)
    assert(outcome.err.contains("--classpath"), outcome.err)
    assert(!outcome.err.contains("\tat "), outcome.err)

  test("--classpath resolves the dependencies"):
    val outcome = Cli.run(appArgs("--classpath", lib.classes.toString), base)
    assertEquals(outcome.code, 0, clue = outcome.err)
    assert(units(outcome).exists(_.contains("extapp.App.total")), outcome.out)

  test("--classpath-file resolves the dependencies in the one-line format"):
    val file = Files.writeString(Files.createTempFile("cp", ".txt"), s"${lib.classes}:/no/such\n")
    val outcome = Cli.run(appArgs("--classpath-file", file.toString), base)
    assertEquals(outcome.code, 0, clue = outcome.err)
    assert(units(outcome).exists(_.contains("extapp.App.total")), outcome.out)

  test("--classpath-file resolves the dependencies in the one-entry-per-line format"):
    val file = Files.writeString(Files.createTempFile("cp", ".txt"), s"\n${lib.classes}\n\n")
    val outcome = Cli.run(appArgs("--classpath-file", file.toString), base)
    assertEquals(outcome.code, 0, clue = outcome.err)
    assertEquals(outcome.err, "")

  test("a classpath entry that does not exist gives a warning and the run continues"):
    val outcome = Cli.run(appArgs("--classpath", s"/no/such:${lib.classes}"), base)
    assertEquals(outcome.code, 0, clue = outcome.err)
    assertEquals(outcome.err, "Warning: classpath entry does not exist: /no/such\n")

  test("two TASTy roots and two coverage directories give one merged report"):
    val args = List(
      "--tasty",
      lib.classes.toString,
      "--tasty",
      app.classes.toString,
      "--coverage",
      lib.coverage.toString,
      "--coverage",
      app.coverage.toString,
      "--all"
    )
    val rows = units(Cli.run(args, base))
    assert(rows.exists(_.contains("extlib.Base.greet")), rows.toString)
    assert(rows.exists(_.contains("extapp.App.total")), rows.toString)
    assert(rows.forall(!_.contains("cov=1.00")), rows.toString)

  test("a missing second coverage directory is an error"):
    val outcome = Cli.run(appArgs("--coverage", "/no/such"), base)
    assertEquals(outcome.code, Cli.ErrorCode)
    assert(outcome.err.contains("/no/such"))
end ClasspathTest
