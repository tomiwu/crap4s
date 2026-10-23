package crap4s.input

import crap4s.core.Error
import munit.FunSuite

import java.nio.file.Files

class InputFilesTest extends FunSuite:
  test("an invalid path gives BadPath"):
    val bad = "bad\u0000path"
    assertEquals(fileSystem.isDirectory(bad), Left(Error.BadPath(bad)))
    assertEquals(fileSystem.exists(bad), Left(Error.BadPath(bad)))
    assert(fileSystem.readString(bad) match
      case Left(_: Error.CannotRead) => true;
      case _ => false)

  test("a directory and a missing path give Right"):
    val dir = Files.createTempDirectory("crap4s-files").toString
    assertEquals(fileSystem.isDirectory(dir), Right(true))
    assertEquals(fileSystem.exists(dir + "/none"), Right(false))

  test("an unreadable file gives CannotRead"):
    val dir = Files.createTempDirectory("crap4s-files").toString
    assert(fileSystem.readString(dir) match
      case Left(_: Error.CannotRead) => true;
      case _ => false)

  test("splitExisting separates the missing entries"):
    val dir = Files.createTempDirectory("crap4s-files").toString
    assertEquals(
      splitExisting(List(dir, dir + "/none"), fileSystem),
      Right((List(dir), List(dir + "/none")))
    )

  test("splitExisting passes an invalid path as Left"):
    assert(splitExisting(List("a\u0000b"), fileSystem).isLeft)
end InputFilesTest
