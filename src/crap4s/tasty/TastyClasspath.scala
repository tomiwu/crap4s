package crap4s.tasty

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path

object TastyClasspath:

  val JreLabel: String = "jrt:/"

  def jreModulePath: Path =
    FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules", "java.base")

  def normalizedTastyDir(tastyDir: Path): Path =
    tastyDir.toAbsolutePath.normalize

  def entryLabels(tastyDir: Path): List[String] =
    List(normalizedTastyDir(tastyDir).toString, JreLabel)

  def paths(tastyDir: Path): List[Path] =
    List(normalizedTastyDir(tastyDir), jreModulePath)
