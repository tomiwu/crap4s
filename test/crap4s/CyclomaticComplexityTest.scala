package crap4s

import crap4s.tasty.*
import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class CyclomaticComplexityTest extends FunSuite:

  private lazy val loaded: TastyLoadResult =
    val fixture = Path.of("test/fixtures/cc-sample").toAbsolutePath.normalize
    val out = Files.createTempDirectory("crap4s-cc")
    val code = Process(Seq("scala-cli", "compile", fixture.toString, "-d", out.toString, "-S", "3.10.0")).!
    assertEquals(code, 0)
    TastyLoader.load(out).toOption.get

  private lazy val methods: Map[String, LoadedMethod] =
    loaded.methods.map(m => m.name -> m).toMap

  private def cc(name: String): Int =
    CyclomaticComplexity.of(methods(name).body)(using loaded.context)

  private val expected = List(
    "plain" -> 1,
    "oneIf" -> 2,
    "elseIf" -> 3,
    "matchCases" -> 3,
    "matchGuard" -> 3,
    "catchCases" -> 3,
    "catchGuard" -> 2,
    "whileLoop" -> 2,
    "forWithoutGuard" -> 1,
    "forWithGuard" -> 2,
    "onlyFinally" -> 1,
    "andOr" -> 3,
    "nestedLambda" -> 2,
    "inlineIf" -> 2,
    "callsInline" -> 1,
    "andChain" -> 2,
    "orChain" -> 3,
    "ifAnd" -> 3,
    "userOperator" -> 1
  )

  expected.foreach: (name, value) =>
    test(s"CC of $name is $value"):
      assertEquals(cc(name), value)
