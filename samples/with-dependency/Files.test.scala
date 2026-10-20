package sample

class FilesTest extends munit.FunSuite:
  test("extension of a name with a dot"):
    assertEquals(Files.extension("a.txt"), "txt")

  test("extension of a name without a dot"):
    assertEquals(Files.extension("a"), "")

  test("countLines counts the lines of a file"):
    val file = os.temp("one\ntwo\n")
    assertEquals(Files.countLines(file), 2)
