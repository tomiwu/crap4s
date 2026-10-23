package crap4s.input

import crap4s.core.Error
import crap4s.core.attempt
import crap4s.core.describe
import crap4s.core.traverse

import java.nio.file.Files
import java.nio.file.Path

/** Input adapter: the file system checks and reads of the command line. */
trait InputFiles:
  def isDirectory(path: String): Either[Error, Boolean]
  def exists(path: String): Either[Error, Boolean]
  def readString(path: String): Either[Error, String]

val fileSystem: InputFiles = new InputFiles:
  def isDirectory(path: String): Either[Error, Boolean] =
    attempt(Files.isDirectory(Path.of(path)))(_ => Error.BadPath(path))

  def exists(path: String): Either[Error, Boolean] =
    attempt(Files.exists(Path.of(path)))(_ => Error.BadPath(path))

  def readString(path: String): Either[Error, String] =
    attempt(Files.readString(Path.of(path)))(e => Error.CannotRead(path, describe(e)))

/** Splits the entries into the existing ones and the missing ones. */
def splitExisting(
    entries: List[String],
    files: InputFiles
): Either[Error, (List[String], List[String])] =
  traverse(entries)(entry => files.exists(entry).map(entry -> _)).map(checked =>
    val (found, missing) = checked.partition(_._2)
    (found.map(_._1), missing.map(_._1))
  )
