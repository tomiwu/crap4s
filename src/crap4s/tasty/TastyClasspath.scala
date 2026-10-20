package crap4s.tasty

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Path

object TastyClasspath:

  val JreLabel: String = "jrt:/"

  def jreModulePath: Path =
    FileSystems.getFileSystem(URI.create(JreLabel)).getPath("modules", "java.base")

  def normalizedTastyDir(tastyDir: Path): Path =
    tastyDir.toAbsolutePath.normalize

  def entryLabels(tastyDir: Path): List[String] =
    List(normalizedTastyDir(tastyDir).toString, JreLabel)

  def scalaLibraryPath: Path =
    Path.of(classOf[scala.Predef.type].getProtectionDomain.getCodeSource.getLocation.toURI)

  /**
   * Jars on the JVM class path of crap4s. They give the types of the Scala library and of crap4s
   * itself.
   */
  def runtimeJars(tastyDir: Path): List[Path] =
    System
      .getProperty("java.class.path", "")
      .split(java.io.File.pathSeparator)
      .toList
      .filter(_.endsWith(".jar"))
      .map(entry => Path.of(entry).toAbsolutePath.normalize)
      .filter(_ != normalizedTastyDir(tastyDir))

  def paths(tastyDir: Path): List[Path] =
    (List(normalizedTastyDir(tastyDir), jreModulePath, scalaLibraryPath) ++
      runtimeJars(tastyDir)).distinct
end TastyClasspath
