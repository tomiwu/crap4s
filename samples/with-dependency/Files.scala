package sample

object Files:
  def extension(name: String): String =
    if name.contains(".") then name.split('.').last else ""

  def countLines(path: os.Path): Int =
    os.read.lines(path).size
