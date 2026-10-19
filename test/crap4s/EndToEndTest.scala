package crap4s

import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class EndToEndTest extends FunSuite:

  private val base = Path.of("").toAbsolutePath
  private val project = Path.of("testdata").toAbsolutePath.normalize

  private lazy val dirs: (Path, Path) =
    val coverage = Files.createTempDirectory("crap4s-e2e-coverage")
    val opts = Seq("-O", s"-coverage-out:$coverage", "-O", s"-sourceroot:$project")
    val classPath = Process(Seq("scala-cli", "compile", project.toString, "--print-class-path") ++ opts).!!
    val classes = Files.createTempDirectory("crap4s-e2e-classes")
    val mainClasses = Path.of(classPath.trim.takeWhile(_ != ':'))
    Process(Seq("cp", "-r", s"$mainClasses/.", classes.toString)).!!
    assertEquals(Process(Seq("scala-cli", "test", project.toString) ++ opts).!, 0)
    (classes, coverage)

  private def run(extra: String*): Outcome =
    Cli.run(List("--tasty", dirs._1.toString, "--coverage", dirs._2.toString) ++ extra, base)

  private def expected(name: String): String = Files.readString(project.resolve(name))

  test("threshold 6 gives the expected CSV and a code that is not 0"):
    val outcome = run("--threshold", "6")
    assertEquals(outcome.out, expected("expected.csv"))
    assertEquals(outcome.code, Cli.AboveThresholdCode)

  test("--all gives the expected CSV"):
    assertEquals(run("--threshold", "6", "--all").out, expected("expected-all.csv"))

  test("a high threshold gives code 0"):
    assertEquals(run("--threshold", "100000").code, 0)
