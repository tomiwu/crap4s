package crap4s.cli

import crap4s.core.Threshold
import crap4s.output.renderHuman
import crap4s.pipeline.Scored
import munit.FunSuite

class RegistryTest extends FunSuite:

  test("the registry knows agent"):
    val report = reportFor(Format.Agent, Threshold.Default, all = false)
    assertEquals(report(Scored(Nil)).map(_.out), Right("PASS 0 max=0.00\n"))

  test("the registry builds the human writer"):
    val human = reportFor(Format.Human, Threshold.Default, all = false, color = true)
    val expected = renderHuman(true, Nil, Threshold.Default)
    assertEquals(human(Scored(Nil)).map(_.out), Right(expected))

  test("the agent format is the default"):
    assertEquals(defaultFormat, Format.Agent)

  test("parse finds a format by name"):
    assertEquals(parseFormat("human"), Right(Format.Human))

  test("an unknown format lists the known names"):
    val error = parseFormat("xml").left.toOption.get
    assertEquals(error.message, "Unknown format: xml. Known formats: agent, human")
end RegistryTest
