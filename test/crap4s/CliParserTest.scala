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
      Right(CliConfig(List(tasty), List(coverage), Nil, CliConfig.DefaultThreshold, false))
    )

  test("parse threshold as integer or decimal"):
    val (tasty, coverage) = existingDirs("threshold")
    assertEquals(
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--threshold", "6.5")),
      Right(CliConfig(List(tasty), List(coverage), Nil, Threshold.from(6.5).toOption.get, false))
    )

  test("parse --all flag"):
    val (tasty, coverage) = existingDirs("all")
    assertEquals(
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--all")),
      Right(CliConfig(List(tasty), List(coverage), Nil, CliConfig.DefaultThreshold, true))
    )

  test("fail when --tasty is missing"):
    val coverage = Files.createTempDirectory("missing-tasty-coverage").toString
    val result = CliParser.parse(List("--coverage", coverage))
    assertEquals(result, Left(Error.MissingOption("--tasty")))

  test("fail when --coverage is missing"):
    val tasty = Files.createTempDirectory("missing-coverage-tasty").toString
    val result = CliParser.parse(List("--tasty", tasty))
    assertEquals(result, Left(Error.MissingOption("--coverage")))

  test("fail when tasty directory does not exist"):
    val coverage = Files.createTempDirectory("bad-tasty-coverage").toString
    val result = CliParser.parse(List("--tasty", "/no/such/tasty", "--coverage", coverage))
    assertEquals(result, Left(Error.MissingDirectory("/no/such/tasty")))

  test("fail when coverage directory does not exist"):
    val tasty = Files.createTempDirectory("bad-coverage-tasty").toString
    val result = CliParser.parse(List("--tasty", tasty, "--coverage", "/no/such/coverage"))
    assertEquals(result, Left(Error.MissingDirectory("/no/such/coverage")))

  test("--tasty and --coverage can occur more than once"):
    val (tasty, coverage) = existingDirs("many")
    val (tasty2, coverage2) = existingDirs("many2")
    val args = List("--tasty", tasty, "--tasty", tasty2, "--coverage", coverage) ++
      List("--coverage", coverage2)
    assertEquals(
      CliParser.parse(args),
      Right(
        CliConfig(
          List(tasty, tasty2),
          List(coverage, coverage2),
          Nil,
          CliConfig.DefaultThreshold,
          false
        )
      )
    )

  test("--classpath splits entries at colons and can occur more than once"):
    val (tasty, coverage) = existingDirs("cp")
    val result = CliParser.parse(
      List("--tasty", tasty, "--coverage", coverage, "--classpath", "a:b::c", "--classpath", "d")
    )
    assertEquals(result.map(_.classpath), Right(List("a", "b", "c", "d")))

  test("--classpath-file accepts one line or one entry on each line and ignores empty lines"):
    val (tasty, coverage) = existingDirs("cpfile")
    val file = Files.writeString(Files.createTempFile("cp", ".txt"), "a:b\n\nc\n  \nd:e\n")
    val result = CliParser.parse(
      List("--tasty", tasty, "--coverage", coverage, "--classpath-file", file.toString)
    )
    assertEquals(result.map(_.classpath), Right(List("a", "b", "c", "d", "e")))

  test("fail when the --classpath-file file cannot be read"):
    val (tasty, coverage) = existingDirs("cpbad")
    val result = CliParser.parse(
      List("--tasty", tasty, "--coverage", coverage, "--classpath-file", "/no/such/file")
    )
    assert(result.left.exists(_.message.startsWith("Cannot read /no/such/file")), result.toString)

  test("an error has a readable message"):
    assertEquals(Error.UnknownOption("--x").message, "Unknown option: --x")
    assertEquals(
      Error.BadField(2, "line", "a number", "q").message,
      "Damaged scoverage.coverage: record 2: line is not a number: 'q'"
    )

  test("a threshold of 0, a negative threshold and NaN are rejected"):
    val (tasty, coverage) = existingDirs("bad-threshold")
    List("0", "-2", "NaN").foreach: value =>
      assertEquals(
        CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--threshold", value)),
        Left(Error.InvalidThreshold(value))
      )

  test("parse --format agent"):
    val (tasty, coverage) = existingDirs("format")
    val result =
      CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--format", "agent"))
    assertEquals(result.map(_.format), Right("agent"))

  test("an unknown --format value is an error that lists the known names"):
    val (tasty, coverage) = existingDirs("badformat")
    val result = CliParser.parse(List("--tasty", tasty, "--coverage", coverage, "--format", "xml"))
    assertEquals(result, Left(Error.UnknownFormat("xml", List("agent"))))
    assert(result.left.toOption.get.message.contains("Known formats: agent"))
end CliParserTest
