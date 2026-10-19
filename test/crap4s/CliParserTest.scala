package crap4s

import munit.FunSuite
import java.nio.file.Files

class CliParserTest extends FunSuite:

  private def existingDirs(prefix: String): (String, String) =
    val tasty = Files.createTempDirectory(s"$prefix-tasty").toString
    val coverage = Files.createTempDirectory(s"$prefix-coverage").toString
    (tasty, coverage)

  test("parse required options with defaults"):
    val (tasty, coverage) = existingDirs("parse")
    val result = CliParser.parse(List("--tasty", tasty, "--coverage", coverage))
    assertEquals(
      result,
      Right(CliConfig(tasty, coverage, CliConfig.DefaultThreshold, false))
    )

  test("parse threshold as integer or decimal"):
    val (tasty, coverage) = existingDirs("threshold")
    assertEquals(
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--threshold", "6.5")),
      Right(CliConfig(tasty, coverage, 6.5, false))
    )

  test("parse --all flag"):
    val (tasty, coverage) = existingDirs("all")
    assertEquals(
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--all")),
      Right(CliConfig(tasty, coverage, CliConfig.DefaultThreshold, true))
    )

  test("fail when --tasty is missing"):
    val coverage = Files.createTempDirectory("missing-tasty-coverage").toString
    val result = CliParser.parse(List("--coverage", coverage))
    assert(result.isLeft, clue = result)
    assert(result.swap.toOption.get.contains("--tasty"), clue = result)

  test("fail when --coverage is missing"):
    val tasty = Files.createTempDirectory("missing-coverage-tasty").toString
    val result = CliParser.parse(List("--tasty", tasty))
    assert(result.isLeft, clue = result)
    assert(result.swap.toOption.get.contains("--coverage"), clue = result)

  test("fail when tasty directory does not exist"):
    val coverage = Files.createTempDirectory("bad-tasty-coverage").toString
    val result = CliParser.parse(List("--tasty", "/no/such/tasty", "--coverage", coverage))
    assert(result.isLeft)

  test("fail when coverage directory does not exist"):
    val tasty = Files.createTempDirectory("bad-coverage-tasty").toString
    val result = CliParser.parse(List("--tasty", tasty, "--coverage", "/no/such/coverage"))
    assert(result.isLeft)
