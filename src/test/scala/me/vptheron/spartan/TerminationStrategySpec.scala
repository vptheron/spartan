package me.vptheron.spartan

import org.scalacheck.Gen
import Generators._

class TerminationStrategySpec extends SpartanSpec {


  "A never ending termination strategy" should {

    "never terminate" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.neverTerminates
          .shouldTerminate(attemptCount, delay) shouldBe false
      }
    }
  }

  "A forever terminates strategy" should {

    "always terminate" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.alwaysTerminates
          .shouldTerminate(attemptCount, delay) shouldBe true
      }
    }
  }

  "A time based termination strategy" should {

    "not terminate if the delay has not expired" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.maxDuration(delay + oneMillis)
          .shouldTerminate(attemptCount, delay) shouldBe false
      }
    }

    "terminate if the delay has expired" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.maxDuration(delay - oneMillis)
          .shouldTerminate(attemptCount, delay) shouldBe true
      }
    }
  }

  "An attempt count based termination strategy" should {

    "not terminate if the attempt count has not been reached" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        forAll(Gen.choose(attemptCount + 1, 200)) { maxCount =>
          TerminationStrategy.maxAttempts(maxCount)
            .shouldTerminate(attemptCount, delay) shouldBe false
        }
      }
    }

    "terminate if the attempt count has been reached" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        forAll(Gen.choose(0, attemptCount - 1)) { maxCount =>
          TerminationStrategy.maxAttempts(maxCount)
            .shouldTerminate(attemptCount, delay) shouldBe true
        }
      }
    }
  }

  "A terminatesIfAtLeastOne strategy combination" should {

    "terminate if at least one of the strategies terminates" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.terminatesIfAtLeastOne(
          TerminationStrategy.alwaysTerminates, TerminationStrategy.neverTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe true
        TerminationStrategy.terminatesIfAtLeastOne(
          TerminationStrategy.alwaysTerminates, TerminationStrategy.alwaysTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe true
        TerminationStrategy.terminatesIfAtLeastOne(
          TerminationStrategy.neverTerminates, TerminationStrategy.alwaysTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe true
      }
    }

    "not terminate if none of the strategies terminates" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.terminatesIfAtLeastOne(
          TerminationStrategy.neverTerminates, TerminationStrategy.neverTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe false
      }
    }
  }

  "A terminatesIfBoth strategy combination" should {

    "terminate if both of the strategies terminates" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.terminatesIfBoth(
          TerminationStrategy.alwaysTerminates, TerminationStrategy.alwaysTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe true
      }
    }

    "not terminate if either of the strategies doesn't terminate" in {
      forAll(Gen.choose(1, 100), wideFiniteDurationGen) { (attemptCount, delay) =>
        TerminationStrategy.terminatesIfBoth(
          TerminationStrategy.neverTerminates, TerminationStrategy.neverTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe false
        TerminationStrategy.terminatesIfBoth(
          TerminationStrategy.alwaysTerminates, TerminationStrategy.neverTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe false
        TerminationStrategy.terminatesIfBoth(
          TerminationStrategy.neverTerminates, TerminationStrategy.alwaysTerminates)
          .shouldTerminate(attemptCount, delay) shouldBe false
      }
    }
  }

}
