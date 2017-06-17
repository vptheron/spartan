package me.vptheron.spartan

import java.util.concurrent.{ThreadLocalRandom, TimeUnit}

import scala.concurrent.duration.FiniteDuration

/**
  * A DelayStrategy is used to compute retry delays based on past failure count.
  */
trait DelayStrategy {

  def nextDelay(pastAttempts: Int): FiniteDuration

}

object DelayStrategy {

  private val Zero = FiniteDuration(0, TimeUnit.NANOSECONDS)

  /**
    * A DelayStrategy of zero
    */
  val NoDelay: DelayStrategy = constantDelay(Zero)

  /**
    * A DelayStrategy that will always return `delay` regardless of attempt count.
    */
  def constantDelay(delay: FiniteDuration): DelayStrategy = (_: Int) => delay

  /**
    * A linearly increasing DelayStrategy.  After the 1st failure the delay is `delay`,
    * after the second failure it is 2 * `delay`, after the 3rd it is 3 * `delay`, etc.
    */
  def linearDelay(delay: FiniteDuration): DelayStrategy = (pastAttempts: Int) =>
    delay * pastAttempts.toLong

  /**
    * An exponentially increasing DelayStrategy.  The first delay is `delay` and after
    * each attempt the previous delay is multiplied by `factor`.  For example, with
    * `delay` = 1 second and factor = 1.5 the various delays will be: 1s, 1.5s, 2.25s,
    * 3.375s, etc.
    */
  def exponentialDelay(delay: FiniteDuration, factor: Double): DelayStrategy =
    (pastAttempts: Int) => {
      val next = delay * Math.pow(factor, (pastAttempts - 1).toDouble)
      FiniteDuration(next.length, next.unit)
    }

  /**
    * Adds a boundary to the next delay that can be returned by the given strategy.
    * The minimum of `strategy.nextDelay` and `maxDelay` is returned.
    */
  def withMaxDelay(strategy: DelayStrategy, maxDelay: FiniteDuration): DelayStrategy =
    (pastAttempts: Int) => maxDelay.min(strategy.nextDelay(pastAttempts))

  /**
    * Adds a random jitter to the given strategy.  For each next delay computed by the
    * strategy, a random value between [-jitter, jitter] is added to the delay.
    *
    * If the jitter is greater than the computed delay, the returned delay is 0 (i.e.
    * it will never be negative)
    */
  def withJitter(strategy: DelayStrategy, jitter: FiniteDuration): DelayStrategy =
    (pastAttempts: Int) => {
      val next = strategy.nextDelay(pastAttempts) +
        FiniteDuration(
          ThreadLocalRandom.current().nextLong(-jitter.length, jitter.length + 1),
          jitter.unit)
      Zero.max(next)
    }

}