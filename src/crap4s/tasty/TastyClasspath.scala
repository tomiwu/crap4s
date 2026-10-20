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

  def entryLabels(tastyDirs: List[Path]): List[String] =
    tastyDirs.map(normalizedTastyDir(_).toString) :+ JreLabel

  def scalaLibraryPath: Path =
    Path.of(classOf[scala.Predef.type].getProtectionDomain.getCodeSource.getLocation.toURI)

  /**
   * Jars on the JVM class path of crap4s. They give the types of the Scala library and of crap4s
   * itself.
   */
  def runtimeJars(tastyDirs: List[Path]): List[Path] =
    System
      .getProperty("java.class.path", "")
      .split(java.io.File.pathSeparator)
      .toList
      .filter(_.endsWith(".jar"))
      .map(entry => Path.of(entry).toAbsolutePath.normalize)
      .filterNot(tastyDirs.map(normalizedTastyDir).contains)

  /** The TASTy roots come first. Then come the JRE, the Scala library and the extra entries. */
  def paths(tastyDirs: List[Path], extra: List[Path]): List[Path] =
    (tastyDirs.map(normalizedTastyDir) ++ List(jreModulePath, scalaLibraryPath) ++
      runtimeJars(tastyDirs) ++ extra.map(normalizedTastyDir)).distinct
end TastyClasspath
