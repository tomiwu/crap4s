package crap4s.input.scoverage

import java.nio.file.Path

/**
 * Returns a normalized path with `/` separators. A path inside `baseDir` becomes relative to
 * `baseDir`.
 */
def normalizePath(raw: String, baseDir: Option[Path]): String =
  val path = Path.of(raw.replace('\\', '/')).normalize
  val relative = baseDir.map(_.toAbsolutePath.normalize).filter(base =>
    path.isAbsolute && path.startsWith(base)
  )
  relative.fold(path)(_.relativize(path)).toString.replace('\\', '/')
