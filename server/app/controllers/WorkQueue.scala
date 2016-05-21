package controllers

import shared._
import org.joda.time.{Interval, DateTime}
import shared.{JobsStatus, Result, JobID, WorkItem}

object WorkQueue {
  val DefaultTimeoutMillis = 5000

  private val workItems = new collection.mutable.Queue[WorkItem]
  private var pendingJobs = Map.empty[JobID, PendingJob]
  private var results = Seq.empty[Result]
  private var jobCount = 0L
  private var failedJobs = 0

  createMockJobs()


  def dequeue(): WorkItem = synchronized {
    Scheduler.run()
    val workItem = workItems.dequeue()
    pendingJobs += workItem.id -> PendingJob(workItem, new DateTime)
    workItem
  }

  def addJob(jsCode: String, returnType: WorkItemReturnType): JobID = synchronized {
    val id = JobID(jobCount)
    jobCount += 1
    workItems.enqueue(WorkItem(id, jsCode, returnType))
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
    addJob(JavaScripts.nextPrimeFinder(101918, 101920), ReturnOptionalDouble) //no primes in this range
    addJob(JavaScripts.nextPrimeFinder(101918, 101921), ReturnOptionalDouble) //101921 is prime
    for (i <- 1 to 100) {
      addJob(JavaScripts.estimatePI, ReturnDouble)
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

