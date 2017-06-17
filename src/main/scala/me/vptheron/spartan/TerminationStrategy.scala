package me.vptheron.spartan

import scala.concurrent.duration.FiniteDuration

/**
  * A TerminationStrategy is used to decide if a computation should end or not.
  */
trait TerminationStrategy {

  def shouldTerminate(pastAttempts: Int, startToNextAttempt: FiniteDuration): Boolean

}

object TerminationStrategy {

  /**
    * Creates a TerminationStrategy that will call for termination after the provided
    * attempt count has been reached.
    */
  def maxAttempts(maxAttempts: Int): TerminationStrategy =
    (pastAttempts: Int, _: FiniteDuration) => pastAttempts >= maxAttempts

  /**
    * Creates a TerminationStrategy that will call for termination after the
    * provided duration has elapsed since the beginning of the computation.
    */
  def maxDuration(d: FiniteDuration): TerminationStrategy =
    (_: Int, startToNextAttempt: FiniteDuration) => startToNextAttempt >= d

  /**
    * A TerminationStrategy that will never terminate.
    */
  val neverTerminates: TerminationStrategy = (_: Int, _: FiniteDuration) => false

  /**
    * A TerminationStrategy that will always terminate.
    */
  val alwaysTerminates: TerminationStrategy = (_: Int, _: FiniteDuration) => true

  /**
    * Creates a TerminationStrategy that will call for termination if at least one
    * of the given strategies calls for termination.
    */
  def terminatesIfAtLeastOne(a: TerminationStrategy, b: TerminationStrategy): TerminationStrategy =
    (pastAttempts: Int, startToNextAttempt: FiniteDuration) =>
      a.shouldTerminate(pastAttempts, startToNextAttempt) || b.shouldTerminate(pastAttempts, startToNextAttempt)

  /**
    * Creates a TerminationStrategy that will call for termination only if both of
    * the given strategies call for termination.
    */
  def terminatesIfBoth(a: TerminationStrategy, b: TerminationStrategy): TerminationStrategy =
    (pastAttempts: Int, startToNextAttempt: FiniteDuration) =>
      a.shouldTerminate(pastAttempts, startToNextAttempt) && b.shouldTerminate(pastAttempts, startToNextAttempt)

}