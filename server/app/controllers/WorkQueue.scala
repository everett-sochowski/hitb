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
  private var aggregateJobResults = new TypedMap()
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
    println(s"Aggregate job ${job.id} completed. Results = " + job.reduce(subResults))
  }

  def status = JobsStatus(
    workItems.size,
    pendingJobs.size,
    for {
      (aggregateJob, pending) <- pendingAggregateJobs.toSeq
      nPending = pending.size
      nCompleted = aggregateJobResults.get(aggregateJob).size
    } yield AggregateJobStatus(aggregateJob.id, nCompleted, nPending),
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

    addJob(JavaScripts.leftPad(s"hello", 8), None, ReturnString)

    addAggregateJob(ReturnOptionalDouble, primeReducer _, JavaScripts.nextPrimeFinder(101918, 101920), JavaScripts.nextPrimeFinder(101921, 101922))

    addAggregateJob(ReturnDouble, piReducer _, Seq.fill(10)(JavaScripts.estimatePI): _*)


    for (i <- 1 to 100) {
      addJob(JavaScripts.leftPad(s"hello_$i", 8), None, ReturnString)
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

  def leftPad(str: String, len: Int, char: Option[Char] = None) =
    s"""var cache = [
       |  '',
       |  ' ',
       |  '  ',
       |  '   ',
       |  '    ',
       |  '     ',
       |  '      ',
       |  '       ',
       |  '        ',
       |  '         '
       |];
       |
       |function leftPad (str, len, ch) {
       |  // convert `str` to `string`
       |  str = str + '';
       |
       |  // doesn't need to pad
       |  len = len - str.length;
       |  if (len <= 0) return str;
       |
       |  // convert `ch` to `string`
       |  if (!ch && ch !== 0) ch = ' ';
       |  ch = ch + '';
       |  if (ch === ' ' && len < 10) return cache[len] + str;
       |  var pad = '';
       |  while (true) {
       |    if (len & 1) pad += ch;
       |    len >>= 1;
       |    if (len) ch += ch;
       |    else break;
       |  }
       |  return pad + str;
       |}
       |
       |console.log("Calculation Started -- Left pad");
       |var padded = ${char.fold(s"leftPad('$str', $len);")(c => s"leftPad('$str', $len, '$char');")}
       |console.log("Calculation finished: " + padded);
       |padded
     """.stripMargin
}

