package crap4s.input.tasty

import crap4s.core.Error
import crap4s.core.attempt
import crap4s.core.describe
import crap4s.core.traverse

import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

val JreLabel: String = "jrt:/"

def parsePath(value: String): Either[Error, Path] =
  attempt(Path.of(value))(_ => Error.BadPath(value))

private def setupFailed(e: Throwable): Error = Error.ClasspathSetupFailed(describe(e))

def jreModulePath: Either[Error, Path] =
  attempt(FileSystems.getFileSystem(URI.create(JreLabel)).getPath("modules", "java.base"))(
    setupFailed
  )

def normalizedTastyDir(tastyDir: Path): Path =
  tastyDir.toAbsolutePath.normalize

def entryLabels(tastyDirs: List[Path]): List[String] =
  tastyDirs.map(normalizedTastyDir(_).toString) :+ JreLabel

def scalaLibraryPath: Either[Error, Path] =
  attempt(Path.of(classOf[scala.Predef.type].getProtectionDomain.getCodeSource.getLocation.toURI))(
    setupFailed
  ).flatMap(asJar)

/**
 * tasty-query accepts only directories and files that end with `.jar`. The release file is named
 * `crap4s`, so this method copies a file without the suffix to a temporary `.jar` file.
 */
def asJar(path: Path): Either[Error, Path] =
  if Files.isDirectory(path) || path.toString.endsWith(".jar") then Right(path)
  else attempt(copyToTempJar(path))(setupFailed)

private def copyToTempJar(path: Path): Path =
  val copy = Files.createTempFile("crap4s-", ".jar")
  copy.toFile.deleteOnExit()
  Files.copy(path, copy, StandardCopyOption.REPLACE_EXISTING)

/**
 * Jars on the JVM class path of crap4s. They give the types of the Scala library and of crap4s
 * itself.
 */
def runtimeJars(tastyDirs: List[Path], javaClassPath: String): Either[Error, List[Path]] =
  traverse(
    javaClassPath
      .split(java.io.File.pathSeparator)
      .toList
      .filter(_.endsWith(".jar"))
  )(parsePath)
    .map(_.map(normalizedTastyDir).filterNot(tastyDirs.map(normalizedTastyDir).contains))

/** The TASTy roots come first. Then come the JRE, the Scala library and the extra entries. */
def classpathPaths(
    tastyDirs: List[Path],
    extra: List[Path],
    javaClassPath: String
): Either[Error, List[Path]] =
  for
    jre <- jreModulePath
    library <- scalaLibraryPath
    jars <- runtimeJars(tastyDirs, javaClassPath)
  yield (tastyDirs.map(normalizedTastyDir) ++ List(jre, library) ++ jars ++
    extra.map(normalizedTastyDir)).distinct
