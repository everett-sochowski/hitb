package shared


case class JobsStatus(
  workItems: Int,
  pendingJobs: Int,
  failedJobs: Int,
  results: Seq[Result]
)

