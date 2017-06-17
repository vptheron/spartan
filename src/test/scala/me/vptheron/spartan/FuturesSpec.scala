package me.vptheron.spartan

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, RejectedExecutionException, TimeoutException}

import me.vptheron.spartan.Futures._
import me.vptheron.spartan.Generators._

import scala.concurrent.ExecutionContext.Implicits.global

class FuturesSpec extends SpartanSpec {

  "safeRun" should {
    "catch exceptions thrown before returning a Future" in {
      val e = new Exception("nope")
      val failingF = () => throw e
      whenReady(safeRun(failingF)().failed) { t =>
        t shouldEqual e
      }
    }
  }

  "withDelay" should {

    "delay the execution of the given future" in {
      forAll(finiteDurationGen) { d =>
        val futureBegins = new AtomicReference(0L)
        val start = System.currentTimeMillis()

        val result = withDelay(futureF {
          futureBegins.set(System.currentTimeMillis())
          42
        }, d)

        whenReady(result()) { i =>
          i shouldEqual 42
          ((futureBegins.get - start) >= d.toMillis) shouldBe true
        }
      }
    }

    "return a failed future if the scheduling fails" in {
      val scheduler = Executors.newSingleThreadScheduledExecutor()
      scheduler.shutdownNow()
      val f = withDelay(futureF(42), oneMillis)(scheduler)

      whenReady(f().failed) { e =>
        e shouldBe a[RejectedExecutionException]
      }
    }

  }

  "withTimeout" should {

    "return the future's result if it terminates on time" in {
      forAll(finiteDurationGen) { d =>
        val fut = withTimeout(futureF {
          Thread.sleep(d.toMillis)
          42
        }, d * 2)

        whenReady(fut())(_ shouldEqual 42)
      }
    }

    "timeout if the Future is too long to complete" in {
      forAll(finiteDurationGen) { d =>
        val fut = withTimeout(futureF {
          Thread.sleep(d.toMillis)
          42
        }, d / 2)

        whenReady(fut().failed) { e =>
          e shouldBe a[TimeoutException]
          e.getMessage shouldEqual s"Execution timed out after ${d / 2}"
        }
      }

    }
  }

  "firstSuccessful" should {

    "return the first successful evaluation" in {
      val f1 = futureF {
        Thread.sleep(100)
        41
      }
      val f2 = futureF {
        Thread.sleep(10)
        42
      }
      val f3 = futureF {
        Thread.sleep(50)
        43
      }

      val fut = firstSuccessful(List(f1, f2, f3))
      fut() map (_ shouldEqual 42)
    }

    "return the single successful evaluation" in {
      val f1 = futureF {
        Thread.sleep(10)
        throw new Exception("Failed")
      }
      val f2 = futureF {
        Thread.sleep(30)
        throw new Exception("Failed")
      }
      val f3 = futureF {
        Thread.sleep(50)
        42
      }

      val fut = firstSuccessful(List(f1, f2, f3))
      fut() map (_ shouldEqual 42)
    }

    "fail with the last failure if all futures fail" in {
      val f1 = futureF {
        Thread.sleep(10)
        throw new Exception("Failed")
      }
      val f2 = futureF {
        Thread.sleep(500)
        throw new Exception("Sorry")
      }
      val f3 = futureF {
        Thread.sleep(30)
        throw new Exception("Failed")
      }

      val fut = firstSuccessful(List(f1, f2, f3))
      whenReady(fut().failed) { e =>
        e shouldBe a[Exception]
        e.getMessage shouldEqual "Sorry"
      }
    }

  }

}
