package crap4s.cli

import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.jdk.CollectionConverters.*
import scala.sys.process.*

class WerrorTest extends FunSuite:

  private val fixtureDir: Path =
    Path.of("test/fixtures/werror-sample").toAbsolutePath.normalize

  private def optionLines(file: Path): List[String] =
    Files.readAllLines(file).asScala.toList.filter(_.startsWith("//> using options"))

  test("the fixture uses the same compiler options as project.scala"):
    val main = optionLines(Path.of("project.scala"))
    assert(main.contains("//> using options -Werror"))
    assertEquals(optionLines(fixtureDir.resolve("project.scala")), main)

  test("a compiler warning fails the build"):
    val out = Files.createTempDirectory("crap4s-werror")
    val output = new StringBuilder
    val logger = ProcessLogger(line => output.append(line).append('\n'): Unit)
    val code = Process(
      Seq("scala-cli", "compile", "--server=false", fixtureDir.toString, "-d", out.toString)
    ).!(logger)
    assertNotEquals(code, 0, clue = output.toString)
    assert(output.toString.contains("unused import"), clue = output.toString)
end WerrorTest
