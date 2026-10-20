package crap4s

import crap4s.tasty.TastyClasspath
import munit.FunSuite

import java.nio.file.Files
import java.nio.file.Path

class TastyClasspathTest extends FunSuite:

  test("asJar keeps a directory") {
    val dir = Files.createTempDirectory("crap4s-dir")
    assertEquals(TastyClasspath.asJar(dir), dir)
  }

  test("asJar keeps a file that ends with .jar") {
    val jar = Files.createTempFile("crap4s-", ".jar")
    assertEquals(TastyClasspath.asJar(jar), jar)
  }

  test("asJar copies a file without the .jar suffix") {
    val file = Files.createTempFile("crap4s-", "")
    Files.writeString(file, "content")
    val result = TastyClasspath.asJar(file)
    assert(result.toString.endsWith(".jar"))
    assertEquals(Files.readString(result), "content")
  }
end TastyClasspathTest
