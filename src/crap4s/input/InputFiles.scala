package crap4s.input

import crap4s.core.Error

import java.nio.file.Files
import java.nio.file.Path
import scala.util.Try

/** Input adapter: the file system checks and reads of the command line. */
trait InputFiles:
  def isDirectory(path: String): Boolean
  def exists(path: String): Boolean
  def readString(path: String): Either[Error, String]

val fileSystem: InputFiles = new InputFiles:
  def isDirectory(path: String): Boolean = Files.isDirectory(Path.of(path))

  def exists(path: String): Boolean = Files.exists(Path.of(path))

  def readString(path: String): Either[Error, String] =
    Try(Files.readString(Path.of(path))).toEither.left
      .map(e => Error.CannotRead(path, e.getMessage))
