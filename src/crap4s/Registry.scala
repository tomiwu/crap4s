package crap4s

/** Maps the values of command line flags to implementations. */
private[crap4s] object Registry:
  val DefaultFormat: String = "csv"

  val writers: Map[String, ReportWriter] = Map("csv" -> CsvWriter)

  def writer(name: String): Either[Error, ReportWriter] =
    writers.get(name).toRight(Error.UnknownFormat(name, writers.keys.toList.sorted))
