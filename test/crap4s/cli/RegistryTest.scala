package crap4s.cli

import crap4s.core.Threshold
import crap4s.output.agentReport
import crap4s.output.renderHuman
import munit.FunSuite

class RegistryTest extends FunSuite:

  test("the registry knows agent"):
    assertEquals(reportFor(Format.Agent), agentReport)

  test("the registry builds the human writer"):
    val human = reportFor(Format.Human, color = true)
    val expected = renderHuman(true, Nil, Threshold.Default)
    assertEquals(human.run(Nil, Threshold.Default, all = false), expected)

  test("the agent format is the default"):
    assertEquals(defaultFormat, Format.Agent)

  test("parse finds a format by name"):
    assertEquals(parseFormat("human"), Right(Format.Human))

  test("an unknown format lists the known names"):
    val error = parseFormat("xml").left.toOption.get
    assertEquals(error.message, "Unknown format: xml. Known formats: agent, human")
end RegistryTest
