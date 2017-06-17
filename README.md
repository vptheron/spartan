# Spartan

[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

<img src="assets/logo.png" width="128" height="128" />

Spartan is a Scala library for handling failures.  It is designed with the following goals in mind (in no particular order):
 
* as few 3rd party dependencies as possible
* a good compromise between being opinionated and flexible
* using FP principles where possible

## Disclaimer

The library is currently experimental and actively worked on.  Versions released as 0.X.Y are still at the exploration 
stage.  Until version 1.0 is reached, the API should be considered highly unstable and candidate to breaking changes.

## Usage

### Futures

The <a href="src/main/scala/me/vptheron/spartan/Futures.scala">Futures</a> module contains convenient functions 
to manipulate `Future` and computations producing a `Future`.

It also introduces the concept of deferred `Future`, aliased `FutureF`.  This is a simple alias for a function
taking 0 arguments, and returning a `Future`.  This is a simple way to manipulate futures and asynchronous computations
while limiting the side-effect of scheduling a `Future` for execution as soon as an instance is created.  It makes it 
possible to write the following:

```scala
val delay: FiniteDuration = ???
val timeout: FiniteDuration = ???
val f = futureF(42)

val resultF = withTimeout(withDelay(f, delay), timeout)

resultF()
```

A deferred computation can be instantiated and manipulated to add timeouts, delays, etc.  It is
then easy to perform the computation at the end.

### RetryStrategy

A <a href="src/main/scala/me/vptheron/spartan/RetryStrategy.scala">RetryStrategy</a> can be used to retry
a potentially failing computation several times.  It is composed of various strategies:

* `DelayStrategy` is used to decide how long to wait between each attempt
* `TerminationStrategy` is used to decide when to give up and return the last failure

Additionally, a `RetryStrategy` can be defined to abort on specific types of `Throwable`.

Each module comes with factory functions to create various strategies.

Examples:

```scala
// A strategy that will give up after 5 failures, and will always wait for `delay`
// between each attempt
RetryStrategy(
  TerminationStrategy.maxAttempts(5),
  DelayStrategy.constantDelay(delay))
  
// A strategy that will retry for `duration`, where the delay between each retry
// will exponentially increase, while remaining lower than `maxDelay`, and where
// a random `jitter` is applied.
RetryStrategy(
  TerminationStrategy.maxDuration(duration),
  DelayStrategy.withJitter(
    DelayStrategy.withMaxDelay(
      DelayStrategy.exponentialDelay(delay, 1.3),
      maxDelay), 
    jitter))
    
// A strategy that will retry either 3 times, or for a max duration of `duration`, 
// whichever happens first.  The delay will grow linearly.  Additionally, if a failure
// happens to be an `IllegalArgumentException`, the computation will return immediately, 
// regardless of the termination strategy.
RetryStrategy(
  TerminationStrategy.terminatesIfAtLeastOne(
    TerminationStrategy.maxAttempts(3),
    TerminationStrategy.maxDuration(duration)),
  DelayStrategy.linearDelay(delay),
  {
    case _: IllegalArgumentException => true
    case _ => false
  })
```

A `RetryStrategy` can be used to perform synchronous, blocking computations, or asynchronous computations:

```scala
val strategy: RetryStrategy = ???

RetryStrategy.attempt(strategy, () => 42)
RetryStrategy.attemptAsync(strategy, Futures.futureF(42))
```

### Circuit Breaker

Coming soon :)

## Contribute

All pull requests welcome.

## License

Copyright 2017 Vincent Theron. Released under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0.html).
