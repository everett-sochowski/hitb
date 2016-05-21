package shared

sealed trait WorkItemReturnType
case object ReturnDouble extends WorkItemReturnType
case object ReturnOptionalDouble extends WorkItemReturnType

case class WorkItem(
  id: JobID,
  parent: Option[AggregateJobId],
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

case class OptionalDoubleResult(
  id: JobID,
  value: Option[Double]
) extends Result

case class JobID(value: Long) extends AnyVal
case class AggregateJobId(value: Long) extends AnyVal

object Shared {
  type JsCode = String
}

