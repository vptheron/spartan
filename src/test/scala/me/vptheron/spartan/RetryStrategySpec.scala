package me.vptheron.spartan

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

class RetryStrategySpec extends SpartanSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  "A blocking retry" should {

    "retry until it succeeds according to the strategies" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val retry = RetryStrategy(termination, delay)

      val counter = new AtomicInteger(0)
      val operation = () => {
        counter.incrementAndGet()
        if (counter.get > 3) 42
        else throw new IllegalArgumentException()
      }

      RetryStrategy.attempt(retry, operation) shouldEqual Success(42)
      counter.get shouldEqual 4
    }

    "return the last failure if strategies stop the retry" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val retry = RetryStrategy(termination, delay)

      val counter = new AtomicInteger(0)
      val failure = new IllegalArgumentException("")
      val operation = () => {
        counter.incrementAndGet()
        throw failure
      }

      RetryStrategy.attempt(retry, operation) shouldEqual Failure(failure)
      counter.get shouldEqual 5
    }

    "return an abortive failure immediately" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val abort: Throwable => Boolean = {
        case _: IllegalArgumentException => true
        case _ => false
      }
      val retry = RetryStrategy(termination, delay, abort)

      val counter = new AtomicInteger(0)
      val failure = new IllegalArgumentException("")
      val operation = () => {
        val runCount = counter.incrementAndGet()
        if (runCount > 2)
          throw failure
        else throw new ArrayIndexOutOfBoundsException()
      }

      RetryStrategy.attempt(retry, operation) shouldEqual Failure(failure)
      counter.get shouldEqual 3
    }

  }

  "A non-blocking retry" should {

    "safely execute futures" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val retry = RetryStrategy(termination, delay)

      val failure = new Exception("NO!")
      val operation: () => Future[Int] = () => throw failure

      whenReady(RetryStrategy.attemptAsync(retry, operation).failed) { result =>
        result shouldEqual failure
      }
    }

    "retry until it succeeds according to the strategies" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val retry = RetryStrategy(termination, delay)

      val counter = new AtomicInteger(0)
      val operation = Futures.futureF {
        counter.incrementAndGet()
        if (counter.get > 3) 42
        else throw new IllegalArgumentException()
      }

      whenReady(RetryStrategy.attemptAsync(retry, operation)) { result =>
        result shouldEqual 42
        counter.get shouldEqual 4
      }
    }

    "return the last failure if strategies stop the retry" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val retry = RetryStrategy(termination, delay)

      val counter = new AtomicInteger(0)
      val failure = new IllegalArgumentException("")
      val operation = Futures.futureF {
        counter.incrementAndGet()
        throw failure
      }

      whenReady(RetryStrategy.attemptAsync(retry, operation).failed){ result =>
        result shouldEqual failure
        counter.get shouldEqual 5
      }
    }

    "return an abortive failure immediately" in {
      val termination = TerminationStrategy.maxAttempts(5)
      val delay = DelayStrategy.constantDelay(FiniteDuration(100, TimeUnit.MILLISECONDS))
      val abort: Throwable => Boolean = {
        case _: IllegalArgumentException => true
        case _ => false
      }
      val retry = RetryStrategy(termination, delay, abort)

      val counter = new AtomicInteger(0)
      val failure = new IllegalArgumentException("")
      val operation = Futures.futureF {
        val runCount = counter.incrementAndGet()
        if (runCount > 2)
          throw failure
        else throw new ArrayIndexOutOfBoundsException()
      }

      whenReady(RetryStrategy.attemptAsync(retry, operation).failed){ result =>
        result shouldEqual failure
        counter.get shouldEqual 3
      }
    }

  }

}
