package crap4s.cli

import crap4s.output.AgentWriter
import crap4s.output.HumanWriter
import munit.FunSuite

class RegistryTest extends FunSuite:

  test("the registry knows agent"):
    assertEquals(Registry.report(Format.Agent), AgentWriter)

  test("the registry builds the human writer"):
    Registry.report(Format.Human, color = true) match
      case _: HumanWriter => ()
      case other => fail(s"Expected HumanWriter, got $other")

  test("the agent format is the default"):
    assertEquals(Format.Default, Format.Agent)

  test("parse finds a format by name"):
    assertEquals(Format.parse("human"), Right(Format.Human))

  test("an unknown format lists the known names"):
    val error = Format.parse("xml").left.toOption.get
    assertEquals(error.message, "Unknown format: xml. Known formats: agent, human")
end RegistryTest
