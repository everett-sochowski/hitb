package controllers

import shared._

object WorkQueue {
  private val workItems = new collection.mutable.Queue[WorkItem]
  private var pendingJobs = Map.empty[JobID, WorkItem]
  private var results = Seq.empty[Result]
  private var jobCount = 0L

  createMockJobs()


  def dequeue(): WorkItem = synchronized {
    val workItem = workItems.dequeue()
    pendingJobs += ((workItem.id, workItem))
    workItem
  }

  def addDoubleJob(jsCode: String): JobID = synchronized {
    val id = JobID(jobCount)
    jobCount += 1
    workItems.enqueue(ReturnDoubleWorkItem(id, jsCode))
    id
  }

  def completeJob(result: Result): Unit = synchronized {
    println(s"Received result: $result")
    if (pendingJobs.contains(result.id)) {
      results :+= result
      pendingJobs -= result.id
    }
  }

  def status = JobsStatus(
    workItems.size,
    pendingJobs.size,
    results
  )

  private def createMockJobs(): Unit = {
    for (i <- 1 to 100) {
      addDoubleJob(JavaScripts.estimatePI)
    }
  }
}

object JavaScripts {
  val estimatePI =
    """console.log("Calculation Started");
      |var r = 5;
      |var points_total = 0;
      |var points_inside = 0;
      |var iterations = 10000000;
      |
      |while (points_total < iterations) {
      |  points_total++;
      |
      |  var x = Math.random() * r * 2 - r;
      |  var y = Math.random() * r * 2 - r;
      |
      |  if (Math.pow(x, 2) + Math.pow(y, 2) < Math.pow(r, 2))
      |    points_inside++;
      |}
      |console.log("Calculation finished");
      |4 * points_inside / points_total;
    """.stripMargin
}

