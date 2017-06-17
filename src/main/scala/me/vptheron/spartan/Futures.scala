package me.vptheron.spartan

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ScheduledExecutorService, TimeoutException}

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

/**
  * A collection of functions to operate on [[scala.concurrent.Future]].
  *
  * Note that the actual type of the abstraction manipulated by these functions is
  * actually `() => Future[A]` or `FutureF[A]`.  This is done to give a little more
  * control to the API consumer over the actual execution of the futures.  Nothing
  * will actually be scheduled for execution until a FutureF is explicitly evaluated.
  */
object Futures extends LazyLogging {

  type FutureF[A] = () => Future[A]

  /**
    * Wraps the given computation to be executed in a deferred Future.
    */
  def futureF[A](a: => A)(implicit ec: ExecutionContext): FutureF[A] =
    () => Future(a)

  /**
    * Wraps the execution of the function in a try block.  Catches any exception that
    * may be thrown by the evaluation of `f` and wraps it in a failed `Future`.
    */
  def safeRun[A](f: FutureF[A]): FutureF[A] = () =>
    Try(f()).fold(t => Future.failed(t), identity)

  /**
    * Adds a delay behavior to the given function.  The `Future` won't start
    * evaluating before the given delay.
    */
  def withDelay[A](f: FutureF[A], delay: FiniteDuration)(
    implicit scheduler: ScheduledExecutorService = Schedulers.DefaultScheduler): FutureF[A] = () => {
    val promise = Promise[A]
    val run = new Runnable {
      def run(): Unit = promise.completeWith(safeRun(f)())
    }
    Try(scheduler.schedule(run, delay.length, delay.unit))
      .fold(t => promise.failure(t), identity)
    promise.future
  }

  /**
    * Adds a timeout behavior to the given function.
    *
    * If the future terminates before the timeout expires, the
    * returned future is completed with its result.  Otherwise, the returned
    * future is failed with a `java.util.concurrent.TimeoutException`.
    *
    * Note that if the timeout is triggered and a failed
    * future is returned, the provided future
    * is **not** canceled.  It may potentially terminate later on.
    */
  def withTimeout[A](f: FutureF[A], timeout: FiniteDuration)(implicit
    ec: ExecutionContext,
    scheduler: ScheduledExecutorService = Schedulers.DefaultScheduler): FutureF[A] = () => {
    val timeoutFut = withDelay(
      () => Future.failed(new TimeoutException(s"Execution timed out after $timeout")),
      timeout)

    Future.firstCompletedOf(safeRun(f)() :: timeoutFut() :: Nil)
  }

  /**
    * Creates a Future function that returns the result of the first
    * successfully completed future,
    * or the last failure if all the given futures are failed.
    */
  def firstSuccessful[A](fs: Seq[FutureF[A]])(implicit ec: ExecutionContext): FutureF[A] = () => {
    val result = Promise[A]
    val count = new AtomicInteger(0)
    val expectedCount = fs.size

    fs foreach (safeRun(_)().onComplete {
      case Success(s) => result.trySuccess(s)
      case Failure(t) =>
        if (count.incrementAndGet() == expectedCount) {
          logger.warn("Last running computation failed.  Failing the `firstSuccessful` computation", t)
          result.tryFailure(t)
        } else {
          logger.warn("Execution failed.  Other computations are still running.", t)
          ()
        }
    })

    result.future
  }

}
