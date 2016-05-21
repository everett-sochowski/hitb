package betterviews

import shared._

import scalatags.Text.all._


object StatusPageView {
  def static(status: JobsStatus): String = Main("Status")(content(status))

  def dynamic() = Main("Status")(onload:="scripts.StatusPage().run()")

  def content(status: JobsStatus) = div(
    h1("Status"),
    h2("Overview"),
    "Unassigned work items: ", status.workItems, br,
    "Pending jobs: ", status.pendingJobs, br,
    "Failed jobs: ", status.failedJobs, br,
    "Completed jobs: ", status.results.size,
    h2("Aggregate Jobs"),
    p(
      ul(
        for (AggregateJobStatus(id, completed, pending) <- status.pendingAggregateJobs)
          yield li(s"Aggregate Job ${id.value}: [$completed/${completed + pending}]")
      )
    ),
    h2("Recent Results"),
    p(
      ul(
        for (result <- status.results.takeRight(10))
          yield resultLi(result)
      )
    )
  )

  def resultLi(r: Result) = r match {
    case DoubleResult(id, value) =>
      li(s"Job ${id.value}: ${value}")
    case OptionalDoubleResult(id, value) =>
      val formattedValue = value.fold("No result found")(_.toString)
      li(s"Job ${id.value}: $formattedValue")
  }
}

