package me.vptheron.spartan

import java.util.concurrent.TimeUnit

import org.scalacheck.Gen

import scala.concurrent.duration.FiniteDuration

object Generators {

  val finiteDurationGen: Gen[FiniteDuration] = Gen.chooseNum[Long](100, 300)
    .map(FiniteDuration(_, TimeUnit.MILLISECONDS))

  val wideFiniteDurationGen: Gen[FiniteDuration] = Gen.chooseNum[Long](1, 10000)
    .map(FiniteDuration(_, TimeUnit.MILLISECONDS))

  def boundedDurationGen(min: FiniteDuration, max: FiniteDuration): Gen[FiniteDuration] =
    Gen.chooseNum[Long](min.toNanos, max.toNanos)
    .map(FiniteDuration(_, TimeUnit.NANOSECONDS))
}
