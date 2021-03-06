package betterviews

import shared._

import scalatags.Text.all._


object StatusPageView {
  def static(status: JobsStatus): String = Main("Status")(content(status))

  def dynamic() = Main("Status")(onload := "scripts.StatusPage().run()")

  def content(status: JobsStatus) = div(
    h1(textAlign:="center", "Status"),
    div(
      display:="flex",
      div(
        flexGrow:="1",
        flexBasis:="0",
        h2("Overview"),
        "Unassigned work items: ", status.workItems, br,
        "Pending jobs: ", status.pendingJobs, br,
        "Failed jobs: ", status.failedJobs, br,
        "Completed jobs: ", status.results.size
      ),
      div(
        flexGrow:="1",
        flexBasis:="0",
        h2("Aggregate Jobs"),
        p(
          for (AggregateJobStatus(id, completed, pending, result) <- status.pendingAggregateJobs)
            yield p(
              strong(s"Aggregate Job ${id.value}: "), s"[$completed/${completed + pending}]",
              result.map(r => Seq[Modifier](br, r))
            )
        )
      ),
      div(
        flexGrow:="1",
        flexBasis:="0",
        h2("Recent Results"),
        p(
          for (result <- status.results.takeRight(20).reverse)
            yield Seq[Modifier](resultItem(result), br)
        )
      )
    )
  )

  def resultItem(r: Result) = r match {
    case DoubleResult(id, value) =>
      s"Job ${id.value}: ${value}"
    case OptionalDoubleResult(id, value) =>
      val formattedValue = value.fold("No result found")(_.toString)
      s"Job ${id.value}: $formattedValue"
    case StringResult(id, value) =>
      s"Job ${id.value}: $value"
  }
}

