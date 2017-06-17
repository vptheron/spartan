package me.vptheron.spartan

import java.util.concurrent.ScheduledExecutorService

import com.typesafe.scalalogging.LazyLogging
import me.vptheron.spartan.Futures.{FutureF, _}

import scala.annotation.tailrec
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object RetryStrategy extends LazyLogging {

  type Timer = () => FiniteDuration

  /**
    * A default timer implementation that uses the system clock
    */
  val SystemTimer: Timer = () => System.nanoTime().nanos

  /**
    * Attempts to perform the given computation in a blocking fashion.  All attempts will
    * reuse and block the calling thread.
    */
  def attempt[A](strategy: RetryStrategy, f: () => A)(implicit timer: Timer = SystemTimer): Try[A] = {
    val start = timer()

    @tailrec
    def loop(pastAttemptCount: Int): Try[A] = Try(f()) match {
      case s@Success(_) => s
      case f@Failure(ex) =>
        evaluateFailure(ex, pastAttemptCount, strategy, start) match {
          case None =>
            logger.warn("Last attempt failed.  No more retry.", ex)
            f
          case Some((newPastAttemptCount, nextDelay)) =>
            logger.warn(s"Attempt $newPastAttemptCount failed, will retry in $nextDelay", ex)
            Thread.sleep(nextDelay.toMillis)
            loop(newPastAttemptCount)
        }
    }

    loop(0)
  }

  /**
    * Attempts to perform the given asynchronous computation according to the given RetryPolicy.
    */
  def attemptAsync[A](strategy: RetryStrategy, f: FutureF[A])(implicit
    ec: ExecutionContext,
    scheduler: ScheduledExecutorService = Schedulers.DefaultScheduler,
    timer: Timer = SystemTimer): Future[A] = {
    val promise = Promise[A]
    val start = timer()

    def loop(pastAttempt: Int): Unit = {
      val result = Futures.safeRun(f)()
      result onComplete {
        case Success(_) => promise.completeWith(result)
        case Failure(ex) =>
          evaluateFailure(ex, pastAttempt, strategy, start) match {
            case None =>
              logger.warn("Last attempt failed, no more retry", ex)
              promise.completeWith(result)
            case Some((newPastAttemptCount, nextDelay)) =>
              logger.warn(s"Attempt $newPastAttemptCount failed, will retry in $nextDelay", ex)
              val delayed = withDelay(futureF(loop(newPastAttemptCount)), nextDelay)
              delayed()
          }
      }
    }

    loop(0)
    promise.future
  }

  private def evaluateFailure[A](
    t: Throwable,
    pastAttemptCount: Int,
    strategy: RetryStrategy,
    start: FiniteDuration)(implicit timer: Timer): Option[(Int, FiniteDuration)] = {
    if (strategy.abortOn(t)) {
      None
    } else {
      val newPastAttemptCount = pastAttemptCount + 1
      val nextDelay = strategy.delayStrategy.nextDelay(newPastAttemptCount)
      val overAllExecutionTime = timer() - start + nextDelay
      if (strategy.terminationStrategy.shouldTerminate(newPastAttemptCount, overAllExecutionTime)) {
        None
      } else {
        Some(newPastAttemptCount -> nextDelay)
      }
    }
  }

}

/**
  * Creates a `RetryStrategy` that will use the provided strategies for termination
  * and delay computation.
  *
  * Additionally, the function `abortOn` can be used to indicate that an exception
  * thrown after an attempt should immediately fail the entire attempt.  The function
  * is evaluated after each failure, and will abort the entire computation if it
  * returns `true`.
  */
case class RetryStrategy(
  terminationStrategy: TerminationStrategy,
  delayStrategy: DelayStrategy,
  abortOn: Throwable => Boolean = _ => false)