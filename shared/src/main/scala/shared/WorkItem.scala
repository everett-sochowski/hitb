package shared

case class WorkItem(
  id: JobID,
  jsCode: String
)

case class Result(
  id: JobID,
  value: Double
)

case class JobID(value: Long) extends AnyVal

