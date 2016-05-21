package betterviews

import shared.{DoubleResult, JobsStatus, Result}

import scalatags.Text.all._


object StatusPageView {
  def static(status: JobsStatus): String = Main("Status")(content(status))

  def dynamic() = Main("Status")(onload:="scripts.StatusPage().run()")

  def resultLi(r: Result) = r match {
    case DoubleResult(id, value) => li(s"Job ${id.value}: ${value}")
  }

  def content(status: JobsStatus) = div(
    h1("Status"),
    "Unassigned work items: ", status.workItems, br,
    "Pending jobs: ", status.pendingJobs, br,
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

