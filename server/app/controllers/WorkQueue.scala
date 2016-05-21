package controllers

import shared._
import org.joda.time.{DateTime, Interval}
import shared.Shared.JsCode
import shared.{JobID, JobsStatus, Result, WorkItem}

object WorkQueue {
  val DefaultTimeoutMillis = 20000

  private val workItems = new collection.mutable.Queue[WorkItem[_]]
  private var pendingJobs = Map.empty[JobID, PendingJob[_]]
  private var pendingAggregateJobs = Map.empty[AggregateJob[_], Set[JobID]]
  private var results = Seq.empty[Result]
  private val aggregateJobResults = new TypedMap()
  private var aggregateResults = Map.empty[AggregateJob[_], String]
  private var jobCount = 0L
  private var failedJobs = 0

  createMockJobs()

  class TypedMap {
    private var mapping = Map.empty[AggregateJob[_], Any]

    def insert[T](key: AggregateJob[T], t: T): Unit = {
      mapping += key -> (get(key) :+ t)
    }

    def get[T](key: AggregateJob[T]): Seq[T] = {
      mapping.getOrElse(key, Seq.empty).asInstanceOf[Seq[T]]
    }
  }

  def dequeue(): WorkItem[_] = synchronized {
    Scheduler.run()
    val workItem = workItems.dequeue()
    pendingJobs += workItem.id -> PendingJob(workItem, new DateTime)
    workItem
  }

  def addAggregateJob[T](returnType: WorkItemReturnType, reduce: Seq[T] => String, subJobs: JsCode*): AggregateJobId = synchronized {
    val aggregateJobId = AggregateJobId(jobCount)
    val aggregateJob = AggregateJob(aggregateJobId, reduce)
    jobCount += 1
    val subJobIds = subJobs.map(addJob(_, Some(aggregateJob), returnType))
    pendingAggregateJobs += aggregateJob -> subJobIds.toSet
    aggregateJobId
  }

  def addJob[T](jsCode: String, parent: Option[AggregateJob[T]], returnType: WorkItemReturnType): JobID = synchronized {
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
      updateParentJob(job.asInstanceOf[PendingJob[Result]], result)
    }
  }

  private def updateParentJob[T](completedJob: PendingJob[T], result: T): Unit = synchronized {
    completedJob.jobDefinition.parent.foreach { parent =>

      completedJob.jobDefinition.parent.foreach(parent => aggregateJobResults.insert(parent, result))

      val parentJobs = pendingAggregateJobs.get(parent)
      parentJobs.foreach { subJobs =>
        val newSubJobs = subJobs - completedJob.jobDefinition.id
        pendingAggregateJobs = pendingAggregateJobs.updated(parent, newSubJobs)
        if(newSubJobs.isEmpty) completeAggregateJob(parent)
      }
    }
  }

  private def completeAggregateJob[T](job: AggregateJob[T]) = synchronized {
    val subResults = aggregateJobResults.get(job)
    val result = job.reduce(subResults)
    aggregateResults += job -> result
    println(s"Aggregate job ${job.id} completed. Results = $result")
  }

  def status = JobsStatus(
    workItems.size,
    pendingJobs.size,
    for {
      (aggregateJob, pending) <- pendingAggregateJobs.toSeq
      nPending = pending.size
      nCompleted = aggregateJobResults.get(aggregateJob).size
      result = aggregateResults.get(aggregateJob)
    } yield AggregateJobStatus(aggregateJob.id, nCompleted, nPending, result),
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

    def primeReducer(results : Seq[OptionalDoubleResult]) : String = {
      results.seq.flatMap(_.value) match {
        case Seq() => "No prime found"
        case xs => s"Found prime ${xs.min}"
      }
    }

    def piReducer(results: Seq[DoubleResult]) : String = {
      val avg = results.map(_.value).sum / results.size
      s"Pi is more or less $avg"
    }

    addAggregateJob(ReturnOptionalDouble, primeReducer, JavaScripts.nextPrimeFinder(101918, 101920), JavaScripts.nextPrimeFinder(101921, 101922))

    addAggregateJob(ReturnDouble, piReducer, Seq.fill(10)(JavaScripts.estimatePI): _*)
    addAggregateJob(ReturnDouble, piReducer, Seq.fill(50)(JavaScripts.estimatePI): _*)
    addAggregateJob(ReturnDouble, piReducer, Seq.fill(250)(JavaScripts.estimatePI): _*)

    for (i <- 1 to 50) {
      addJob(JavaScripts.estimatePI, None, ReturnDouble)
    }
  }
}

case class PendingJob[T](
  jobDefinition: WorkItem[T],
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

