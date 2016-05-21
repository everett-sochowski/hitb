package shared


case class JobsStatus(
  workItems: Int,
  pendingJobs: Int,
  results: Seq[Result]
)

