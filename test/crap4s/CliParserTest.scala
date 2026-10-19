package crap4s

import munit.FunSuite
import java.nio.file.Files

class CliParserTest extends FunSuite:

  test("parse required options with defaults"):
    val tasty = Files.createTempDirectory("tasty").toString
    val coverage = Files.createTempDirectory("coverage").toString
    val result = CliParser.parse(List("--tasty", tasty, "--coverage", coverage))
    assertEquals(result, Right(CliConfig(tasty, coverage, 6.0, false)))

  test("parse threshold as integer or decimal"):
    val tasty = Files.createTempDirectory("tasty2").toString
    val coverage = Files.createTempDirectory("coverage2").toString
    assertEquals(
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--threshold", "6.5")),
      Right(CliConfig(tasty, coverage, 6.5, false))
    )

  test("parse --all flag"):
    val tasty = Files.createTempDirectory("tasty3").toString
    val coverage = Files.createTempDirectory("coverage3").toString
    assertEquals(
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--all")),
      Right(CliConfig(tasty, coverage, 6.0, true))
    )

  test("fail when --tasty is missing"):
    val coverage = Files.createTempDirectory("coverage4").toString
    val result = CliParser.parse(List("--coverage", coverage))
    assert(result.isLeft, clue = result)
    assert(result.swap.toOption.get.contains("--tasty"), clue = result)

  test("fail when --coverage is missing"):
    val tasty = Files.createTempDirectory("tasty5").toString
    val result = CliParser.parse(List("--tasty", tasty))
    assert(result.isLeft, clue = result)
    assert(result.swap.toOption.get.contains("--coverage"), clue = result)

  test("fail when tasty directory does not exist"):
    val coverage = Files.createTempDirectory("coverage6").toString
    val result = CliParser.parse(List("--tasty", "/no/such/tasty", "--coverage", coverage))
    assert(result.isLeft)

  test("fail when coverage directory does not exist"):
    val tasty = Files.createTempDirectory("tasty7").toString
    val result = CliParser.parse(List("--tasty", tasty, "--coverage", "/no/such/coverage"))
    assert(result.isLeft)
