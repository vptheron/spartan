package me.vptheron.spartan

import java.util.concurrent.TimeUnit

import me.vptheron.spartan.DelayStrategy._
import me.vptheron.spartan.Generators._
import org.scalacheck.Gen

import scala.concurrent.duration.FiniteDuration

class DelayStrategySpec extends SpartanSpec {


  "noDelay strategy" should {
    "always return 0 for delay" in {
      forAll(Gen.chooseNum(0, 100)) { count =>
        NoDelay.nextDelay(count) shouldEqual zero
      }
    }
  }

  "constantDelay strategy" should {
    "always return the same initial delay" in {
      forAll(Gen.chooseNum(0, 100), wideFiniteDurationGen) { (count, delay) =>
        constantDelay(delay).nextDelay(count) shouldEqual delay
      }
    }
  }

  "linearDelay strategy" should {
    "return delay * pastAttempt" in {
      forAll(Gen.chooseNum(0, 100), wideFiniteDurationGen) { (count, delay) =>
        linearDelay(delay).nextDelay(count) shouldEqual (delay * count.toLong)
      }
    }

    "return a linear sequence" in {
      val strategy = linearDelay(FiniteDuration(1, TimeUnit.SECONDS))
      strategy.nextDelay(1) shouldEqual FiniteDuration(1, TimeUnit.SECONDS)
      strategy.nextDelay(2) shouldEqual FiniteDuration(2, TimeUnit.SECONDS)
      strategy.nextDelay(3) shouldEqual FiniteDuration(3, TimeUnit.SECONDS)
      strategy.nextDelay(4) shouldEqual FiniteDuration(4, TimeUnit.SECONDS)
      strategy.nextDelay(5) shouldEqual FiniteDuration(5, TimeUnit.SECONDS)
    }
  }

  "exponential strategy" should {
    "return an exponential sequence" in {
      val strategy = exponentialDelay(FiniteDuration(1, TimeUnit.SECONDS), 1.5)
      strategy.nextDelay(1) shouldEqual FiniteDuration(1000, TimeUnit.MILLISECONDS)
      strategy.nextDelay(2) shouldEqual FiniteDuration(1500, TimeUnit.MILLISECONDS)
      strategy.nextDelay(3) shouldEqual FiniteDuration(2250, TimeUnit.MILLISECONDS)
      strategy.nextDelay(4) shouldEqual FiniteDuration(3375, TimeUnit.MILLISECONDS)
    }
  }

  "withMaxDelay strategy" should {
    "return the computed delay if it is lower than the max delay" in {
      forAll(wideFiniteDurationGen) { delay =>
        forAll(boundedDurationGen(delay, delay * 10)) { max =>
          withMaxDelay(constantDelay(delay), max).nextDelay(0) shouldEqual delay
        }
      }
    }

    "return the max delay if it is lower than the computed delay" in {
      forAll(wideFiniteDurationGen) { delay =>
        forAll(boundedDurationGen(zero, delay)) { max =>
          withMaxDelay(constantDelay(delay), max).nextDelay(0) shouldEqual max
        }
      }
    }
  }

  "withJitter" should {
    "add a random jitter to the computed delay" in {
      forAll(wideFiniteDurationGen, wideFiniteDurationGen) { (delay, jitter) =>
        val nanos = withJitter(constantDelay(delay), jitter).nextDelay(0).toNanos
        (nanos >= 0) shouldBe true
        (nanos >= delay.minus(jitter).toNanos) shouldBe true
        (nanos <= delay.plus(jitter).toNanos) shouldBe true
      }
    }
  }

}
