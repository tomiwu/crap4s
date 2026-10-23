package crap4s

/** The real class path for the tests that run the tool. */
given Env = Env(
  EnvOps(
    name => Option(System.getenv(name)),
    name => Option(System.getProperty(name)),
    () => java.nio.file.Path.of("").toAbsolutePath.toString
  )
)
