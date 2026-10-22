package crap4s.pipeline

import crap4s.core.Error

extension [A](e: Either[Error, A])
  /** Feeds the value of a Right to the next stage. A Left skips the stage. */
  def via[B](f: A => Either[Error, B]): Either[Error, B] = e.flatMap(f)
