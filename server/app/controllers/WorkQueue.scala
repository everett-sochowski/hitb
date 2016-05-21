package controllers

import shared.{Result, JobID, WorkItem}

object WorkQueue {
  private val workItems = new collection.mutable.Queue[WorkItem]
  private var pendingJobs = Map.empty[JobID, WorkItem]
  private var results = Seq.empty[Result]

  createMockJobs()


  def dequeue(): WorkItem = synchronized {
    val workItem = workItems.dequeue()
    pendingJobs += ((workItem.id, workItem))
    workItem
  }

  def addJob(jobID: JobID, jsCode: String): Unit = synchronized {
    workItems.enqueue(WorkItem(jobID, jsCode))
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
      addJob(JobID(i), JavaScripts.estimatePI)
    }
  }
}

object JavaScripts {
  val estimatePI =
    """console.log("Calculation Started");
      |var r = 5;
      |var points_total = 0;
      |var points_inside = 0;
      |var iterations = 100000000;
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
      |
      |4 * points_inside / points_total;
      |console.log("Calculation finished");
    """.stripMargin
}

case class JobsStatus(
  workItems: Int,
  pendingJobs: Int,
  results: Seq[Result]
)
