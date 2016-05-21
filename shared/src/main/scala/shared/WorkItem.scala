package shared

sealed trait WorkItemReturnType
case object ReturnDouble extends WorkItemReturnType

case class WorkItem(
  id: JobID,
  jsCode: String,
  returnType: WorkItemReturnType
)

sealed trait Result {
  val id: JobID
}

case class DoubleResult(
  id: JobID,
  value: Double
) extends Result

case class JobID(value: Long) extends AnyVal

