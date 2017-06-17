package me.vptheron.spartan

import java.util.concurrent.TimeUnit

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.time.{Millis, Span}

import scala.concurrent.duration.FiniteDuration

trait SpartanSpec extends WordSpec with PropertyChecks with Matchers with ScalaFutures {

  implicit val PatienceConf = PatienceConfig(timeout = scaled(Span(1000, Millis)))

  val zero: FiniteDuration = FiniteDuration(0, TimeUnit.NANOSECONDS)

  val oneMillis = FiniteDuration(1, TimeUnit.MILLISECONDS)
}
