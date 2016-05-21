package controllers

import shared._
import org.joda.time.{DateTime, Interval}
import shared.Shared.JsCode
import shared.{JobID, JobsStatus, Result, WorkItem}

object WorkQueue {
  val DefaultTimeoutMillis = 20000

  private val workItems = new collection.mutable.Queue[WorkItem]
  private var pendingJobs = Map.empty[JobID, PendingJob]
  private var pendingAggregateJobs = Map.empty[AggregateJobId, Set[JobID]]
  private var results = Seq.empty[Result]
  private var aggregateJobResults = Map.empty[AggregateJobId, Seq[Result]]
  private var jobCount = 0L
  private var failedJobs = 0

  createMockJobs()


  def dequeue(): WorkItem = synchronized {
    Scheduler.run()
    val workItem = workItems.dequeue()
    pendingJobs += workItem.id -> PendingJob(workItem, new DateTime)
    workItem
  }

  def addAggregateJob(returnType: WorkItemReturnType, subJobs: JsCode*): AggregateJobId = synchronized {
    val aggregateJobId = AggregateJobId(jobCount)
    jobCount += 1
    val subJobIds = subJobs.map(addJob(_, Some(aggregateJobId), returnType))
    pendingAggregateJobs += aggregateJobId -> subJobIds.toSet
    aggregateJobId
  }

  def addJob(jsCode: String, parent: Option[AggregateJobId], returnType: WorkItemReturnType): JobID = synchronized {
    val id = JobID(jobCount)
    jobCount += 1
    workItems.enqueue(WorkItem(id, parent, jsCode, returnType))
    id
  }

  def completeJob(result: Result): Unit = synchronized {
    println(s"Received result: $result")
    pendingJobs.get(result.id).foreach { job =>
      results :+= result
      pendingJobs -= result.id
      updateParentJob(job, result)
    }
  }

  private def updateParentJob(completedJob: PendingJob, result: Result): Unit = synchronized {
    completedJob.jobDefinition.parent.foreach { parentId =>

      //We trust our clients!
      val results = aggregateJobResults.getOrElse(parentId, Seq.empty)
      aggregateJobResults = aggregateJobResults.updated(parentId, results :+ result)


      val parentJobs = pendingAggregateJobs.get(parentId)
      parentJobs.foreach { subJobs =>
        val newSubJobs = subJobs - completedJob.jobDefinition.id
        pendingAggregateJobs = pendingAggregateJobs.updated(parentId, newSubJobs)
        if(newSubJobs.isEmpty) completeAggregateJob(parentId)
      }
    }
  }

  private def completeAggregateJob(id: AggregateJobId) = synchronized {
    val subResults = aggregateJobResults.getOrElse(id, Set.empty)
    println(s"Aggregate job $id completed. Results = " + subResults)
  }

  def status = JobsStatus(
    workItems.size,
    pendingJobs.size,
    for {
      (id, pending) <- pendingAggregateJobs.toSeq
      nPending = pending.size
      nCompleted = aggregateJobResults.get(id).map(_.size).getOrElse(0)
    } yield AggregateJobStatus(id, nCompleted, nPending),
    failedJobs,
    results
  )

  def tick(): Unit = {
    val timeNow = new DateTime
    val requireRestart =
      for {
        pending <- pendingJobs.values
        elapsedMs = new Interval(pending.startTime, timeNow).toDurationMillis
        if elapsedMs >= DefaultTimeoutMillis
      } yield pending

    for (PendingJob(workItem, _) <- requireRestart) {
      pendingJobs -= workItem.id
      workItems.enqueue(workItem)
      failedJobs += 1
    }
  }

  private def createMockJobs(): Unit = {
    addAggregateJob(ReturnOptionalDouble, JavaScripts.nextPrimeFinder(101918, 101920), JavaScripts.nextPrimeFinder(101921, 101922))
    addAggregateJob(ReturnDouble, Seq.fill(10)(JavaScripts.estimatePI): _*)
    for (i <- 1 to 100) {
      addJob(JavaScripts.estimatePI, None, ReturnDouble)
    }
  }
}

case class PendingJob(
  jobDefinition: WorkItem,
  startTime: DateTime
)

object JavaScripts {

  val estimatePI =
    """console.log("Calculation Started -- Estimate Pi");
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


  def nextPrimeFinder(searchStart: Long, searchEnd: Long) =
    s"""console.log("Calculation Started -- Next Prime");
      |function isPrime(n) {
      |  var halfN = Math.ceil(n / 2);
      |  for(i = 2; i < halfN; i++) {
      |    if(n % i === 0) return false;
      |  }
      |  return true;
      |}
      |
      |function nextPrime(searchStart, searchEnd) {
      |  for(j = searchStart; j <= searchEnd; j++) {
      |    if(isPrime(j)) return j;
      |  }
      |  return null;
      |}
      |
      |var next = nextPrime($searchStart, $searchEnd);
      |console.log("next prime in range: " + next);
      |console.log("Calculation finished");
      |next;
    """.stripMargin
}

