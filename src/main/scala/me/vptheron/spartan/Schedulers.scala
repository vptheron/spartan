package me.vptheron.spartan

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, ScheduledExecutorService, ThreadFactory}

object Schedulers {

  /**
    * A default, single-threaded scheduler.
    */
  val DefaultScheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
    new ThreadFactory {
      private val counter = new AtomicLong(0)

      def newThread(r: Runnable): Thread = {
        val thread = new Thread(r, s"Schedulers-DefaultScheduler-${counter.incrementAndGet()}")
        thread.setDaemon(true)
        thread
      }
    })

}