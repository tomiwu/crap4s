package crap4s.input.tasty

import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path
import scala.sys.process.*

class CyclomaticComplexityTest extends FunSuite:

  private lazy val loaded: TastyLoadResult =
    val fixture = Path.of("test/fixtures/cc-sample").toAbsolutePath.normalize
    val out = Files.createTempDirectory("crap4s-cc")
    val code =
      Process(Seq("scala-cli", "compile", fixture.toString, "-d", out.toString, "-S", "3.10.0")).!
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
    "nestedLambda" -> 1,
    "simpleLambda" -> 1,
    "lambdaInLambda" -> 1,
    "localDefBranchy" -> 1,
    "localDefSimple" -> 1,
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

  private def units(parent: String): List[LoadedMethod] =
    loaded.methods.filter(_.name.startsWith(parent + "$"))

  private def unitCc(unit: LoadedMethod): Int =
    CyclomaticComplexity.ofUnit(unit)(using loaded.context)

  test("top-level lambda with branches is a separate unit"):
    val found = units("nestedLambda")
    assertEquals(found.map(unitCc), List(2))
    assertEquals(found.head.kind, UnitKind.Lambda)
    assert(found.head.name.matches("nestedLambda\\$lambda@\\d+"), found.head.name)
    assert(found.head.position.startOffset < found.head.position.endOffset)

  test("lambda with CC 1 is not a unit"):
    assertEquals(units("simpleLambda"), Nil)

  test("nested lambda is part of the outer lambda"):
    assertEquals(units("lambdaInLambda").map(unitCc), List(3))

  test("local def with branches is a separate unit"):
    val found = units("localDefBranchy")
    assertEquals(found.map(unitCc), List(2))
    assert(found.head.name.matches("localDefBranchy\\$helper@\\d+"), found.head.name)

  test("local def with CC 1 is not a unit"):
    assertEquals(units("localDefSimple"), Nil)
end CyclomaticComplexityTest

class SyntheticCodeTest extends FunSuite:

  private lazy val loaded: TastyLoadResult =
    val fixture = Path.of("test/fixtures/synthetic-sample").toAbsolutePath.normalize
    val out = Files.createTempDirectory("crap4s-synth")
    val code =
      Process(Seq("scala-cli", "compile", fixture.toString, "-d", out.toString, "-S", "3.10.0")).!
    assertEquals(code, 0)
    TastyLoader.load(out).toOption.get

  test("only user code is a unit"):
    val names = loaded.methods.map(m => s"${m.owner}.${m.name}").toSet
    assertEquals(
      names,
      Set("synth.Point.norm", "synth.Color.isRed", "synth.Synth.twice", "synth.Synth.callsTwice")
    )

  test("inline def has its own CC and the call site does not add it"):
    val byName = loaded.methods.map(m => m.name -> m).toMap
    given tastyquery.Contexts.Context = loaded.context
    assertEquals(CyclomaticComplexity.of(byName("twice").body), 2)
    assertEquals(CyclomaticComplexity.of(byName("callsTwice").body), 1)
end SyntheticCodeTest
