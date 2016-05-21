package betterviews

import shared.{DoubleResult, JobsStatus, OptionalDoubleResult, Result}

import scalatags.Text.all._


object StatusPageView {
  def static(status: JobsStatus): String = Main("Status")(content(status))

  def dynamic() = Main("Status")(onload:="scripts.StatusPage().run()")

  def resultLi(r: Result) = r match {
    case DoubleResult(id, value) =>
      li(s"Job ${id.value}: ${value}")
    case OptionalDoubleResult(id, value) =>
      val formattedValue = value.fold("No result found")(_.toString)
      li(s"Job ${id.value}: $formattedValue")
  }

  def content(status: JobsStatus) = div(
    h1("Status"),
    "Unassigned work items: ", status.workItems, br,
    "Pending jobs: ", status.pendingJobs, br,
    "Failed jobs: ", status.failedJobs, br,
    "Completed jobs: ", status.results.size,
    h2("Results"),
    p(
      ul(
        for (result <- status.results)
          yield resultLi(result)
      )
    )
  )
}

