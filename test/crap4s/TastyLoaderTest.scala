package crap4s

import crap4s.tasty.*
import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class TastyLoaderTest extends FunSuite:

  private val fixtureDir: Path =
    Path.of("test/fixtures/tasty-sample").toAbsolutePath.normalize

  private def compileFixture(outputDir: Path): Unit =
    val code = Process(
      Seq(
        "scala-cli",
        "compile",
        fixtureDir.toString,
        "-d",
        outputDir.toString,
        "-S",
        "3.10.0"
      )
    ).!
    assertEquals(code, 0, clue = s"scala-cli compile failed for $fixtureDir")

  private def loadCompiled(outputDir: Path): TastyLoadResult =
    TastyLoader.load(outputDir) match
      case Left(message) => fail(message.message)
      case Right(result) => result

  test("classpath contains tasty output directory and JRE"):
    val out = Files.createTempDirectory("crap4s-classpath")
    compileFixture(out)
    val result = loadCompiled(out)
    val tastyLabel = out.toAbsolutePath.normalize.toString
    assert(result.classpathEntries.contains(tastyLabel))
    assert(result.classpathEntries.contains(TastyClasspath.JreLabel))

  test("lists methods with names, lines, and body trees from compiled fixture"):
    val out = Files.createTempDirectory("crap4s-methods")
    compileFixture(out)
    val result = loadCompiled(out)

    val topNames = result.topLevels.map(_.name).toSet
    assert(topNames.contains("sample.SampleClass"))
    assert(topNames.contains("sample.Sample"))

    val greet = result.methods.find(_.name == "greet").getOrElse(fail("missing greet"))
    assertEquals(greet.position.line, 3)
    assert(greet.body.isDefined, clue = greet)

    val add = result.methods.find(_.name == "add").getOrElse(fail("missing add"))
    assertEquals(add.position.line, 7)
    assert(add.body.isDefined, clue = add)

  test("java.lang.String references do not fail symbol resolution"):
    val out = Files.createTempDirectory("crap4s-jdk")
    compileFixture(out)
    val result = loadCompiled(out)
    val greet = result.methods.find(_.name == "greet").getOrElse(fail("missing greet"))
    assertEquals(greet.owner, "sample.Sample")
end TastyLoaderTest
