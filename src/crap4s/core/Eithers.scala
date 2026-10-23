package crap4s.core

import scala.util.control.NonFatal

/** Applies `f` to each element. Returns the first Left, or all results in order. */
def traverse[A, E, B](items: List[A])(f: A => Either[E, B]): Either[E, List[B]] =
  items
    .foldLeft(Right(Nil): Either[E, List[B]]): (acc, item) =>
      for
        done <- acc
        value <- f(item)
      yield value :: done
    .map(_.reverse)

/** Runs `body`. Turns a non-fatal exception into a Left made by `onError`. */
def attempt[A, E](body: => A)(onError: Throwable => E): Either[E, A] =
  try Right(body)
  catch case NonFatal(e) => Left(onError(e))

/** The message of the exception, or its class name when the message is empty. */
def describe(e: Throwable): String =
  Option(e.getMessage).getOrElse(e.getClass.getSimpleName)
