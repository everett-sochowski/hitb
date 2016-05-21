package shared


case class JobsStatus(
  workItems: Int,
  pendingJobs: Int,
  pendingAggregateJobs: Seq[AggregateJobStatus],
  failedJobs: Int,
  results: Seq[Result]
)

case class AggregateJobStatus(
  id: AggregateJobId,
  completed: Int,
  pending: Int,
  result: Option[String]
)
