package controllers

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object Scheduler {
  private var started: Boolean = false

  def run(): Unit = synchronized {
    if (started) return

    started = true
    runSchedule()
  }

  private def runSchedule(): Unit = Future {
    controllers.WorkQueue.tick()
      Thread.sleep(1000)
      runSchedule()
    }
}

