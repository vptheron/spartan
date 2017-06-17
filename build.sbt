name := "spartan"

// version is stored in version.sbt

description := "A set of utilities for failure management in Scala"

homepage := Some(url("https://github.com/vptheron/spartan"))

startYear := Some(2017)

licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

organization := "me.vptheron"
organizationName := "vptheron.me"
organizationHomepage := Some(url("http://vptheron.me"))

scalaVersion := "2.12.2"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-target:jvm-1.8",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",

  "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
)

coverageMinimum := 90

coverageFailOnMinimum := true

autoAPIMappings := true

addCommandAlias("testAll", ";clean; coverage; test; coverageReport; coverageOff")
