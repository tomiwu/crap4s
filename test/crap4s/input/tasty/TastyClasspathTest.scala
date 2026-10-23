package crap4s.input.tasty

import crap4s.core.Error
import munit.FunSuite

import java.nio.file.Files

class TastyClasspathTest extends FunSuite:

  test("asJar keeps a directory") {
    val dir = Files.createTempDirectory("crap4s-dir")
    assertEquals(asJar(dir), Right(dir))
  }

  test("asJar keeps a file that ends with .jar") {
    val jar = Files.createTempFile("crap4s-", ".jar")
    assertEquals(asJar(jar), Right(jar))
  }

  test("asJar copies a file without the .jar suffix") {
    val file = Files.createTempFile("crap4s-", "")
    Files.writeString(file, "content")
    val result = asJar(file).toOption.get
    assert(result.toString.endsWith(".jar"))
    assertEquals(Files.readString(result), "content")
  }

  test("parsePath gives BadPath for a path with a NUL character") {
    assertEquals(parsePath("a\u0000b"), Left(Error.BadPath("a\u0000b")))
  }

  test("asJar gives ClasspathSetupFailed for a missing file") {
    val missing = Files.createTempDirectory("crap4s-missing").resolve("absent")
    asJar(missing) match
      case Left(_: Error.ClasspathSetupFailed) => ()
      case other => fail(s"unexpected: $other")
  }

  test("tastyUnitsAt gives BadPath for an invalid path") {
    val units = tastyUnitsAt(List("a\u0000b"), Nil, ".", "")
    assertEquals(units(), Left(Error.BadPath("a\u0000b")))
  }

  test("tastyUnitsAt gives TastyLoadFailed for a corrupt tasty file and a corrupt jar") {
    val dir = Files.createTempDirectory("crap4s-corrupt")
    Files.writeString(dir.resolve("Bad.tasty"), "not tasty")
    val jar = Files.createTempFile("crap4s-bad-", ".jar")
    Files.writeString(jar, "not a jar")
    val fromDir = tastyUnitsAt(List(dir.toString), Nil, ".", "")()
    val fromJar = tastyUnitsAt(List(dir.toString), List(jar.toString), ".", "")()
    fromDir match
      case Left(_: Error.TastyLoadFailed) => ()
      case other => fail(s"unexpected: $other")
    fromJar match
      case Left(_: Error.TastyLoadFailed) => ()
      case other => fail(s"unexpected: $other")
  }
end TastyClasspathTest
