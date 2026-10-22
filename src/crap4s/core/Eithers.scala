package crap4s.core

object Eithers:
  /** Applies `f` to each element. Returns the first Left, or all results in order. */
  def traverse[A, E, B](items: List[A])(f: A => Either[E, B]): Either[E, List[B]] =
    items
      .foldLeft(Right(Nil): Either[E, List[B]]): (acc, item) =>
        for
          done <- acc
          value <- f(item)
        yield value :: done
      .map(_.reverse)
