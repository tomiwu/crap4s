package crap4s.input.scoverage

import crap4s.core.Error
import crap4s.core.attempt

import java.nio.file.Path

/**
 * Returns a normalized path with `/` separators. A path inside `baseDir` becomes relative to
 * `baseDir`. An invalid path gives `Error.BadPath`.
 */
def normalizePath(raw: String, baseDir: Option[Path]): Either[Error, String] =
  attempt(normalized(raw, baseDir))(_ => Error.BadPath(raw))

private def normalized(raw: String, baseDir: Option[Path]): String =
  val path = Path.of(raw.replace('\\', '/')).normalize
  val relative = baseDir.map(_.toAbsolutePath.normalize).filter(base =>
    path.isAbsolute && path.startsWith(base)
  )
  relative.fold(path)(_.relativize(path)).toString.replace('\\', '/')
