package shared

sealed trait WorkItem {
  val id: JobID
  val jsCode: String
}

case class ReturnDoubleWorkItem(
  id: JobID,
  jsCode: String
) extends WorkItem

sealed trait Result {
  val id: JobID
}

case class DoubleResult(
  id: JobID,
  value: Double
) extends Result

case class JobID(value: Long) extends AnyVal

