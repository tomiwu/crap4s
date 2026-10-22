package crap4s.input.tasty

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

val JreLabel: String = "jrt:/"

def jreModulePath: Path =
  FileSystems.getFileSystem(URI.create(JreLabel)).getPath("modules", "java.base")

def normalizedTastyDir(tastyDir: Path): Path =
  tastyDir.toAbsolutePath.normalize

def entryLabels(tastyDirs: List[Path]): List[String] =
  tastyDirs.map(normalizedTastyDir(_).toString) :+ JreLabel

def scalaLibraryPath: Path =
  asJar(Path.of(classOf[scala.Predef.type].getProtectionDomain.getCodeSource.getLocation.toURI))

/**
 * tasty-query accepts only directories and files that end with `.jar`. The release file is named
 * `crap4s`, so this method copies a file without the suffix to a temporary `.jar` file.
 */
def asJar(path: Path): Path =
  if Files.isDirectory(path) || path.toString.endsWith(".jar") then path
  else
    val copy = Files.createTempFile("crap4s-", ".jar")
    copy.toFile.deleteOnExit()
    Files.copy(path, copy, StandardCopyOption.REPLACE_EXISTING)

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
def classpathPaths(tastyDirs: List[Path], extra: List[Path]): List[Path] =
  (tastyDirs.map(normalizedTastyDir) ++ List(jreModulePath, scalaLibraryPath) ++
    runtimeJars(tastyDirs) ++ extra.map(normalizedTastyDir)).distinct
