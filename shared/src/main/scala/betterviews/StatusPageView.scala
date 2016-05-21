package betterviews

import shared.JobsStatus

import scalatags.Text.all._


object StatusPageView {
  def static(status: JobsStatus): String = Main("Status")(content(status))

  def dynamic() = Main("Status")(onload:="scripts.StatusPage().run()")

  def content(status: JobsStatus) = div(
    h1("Status"),
    "Unassigned work items: ", status.workItems, br,
    "Pending jobs: ", status.pendingJobs, br,
    "Completed jobs: ", status.results.size,
    h2("Results"),
    p(
      ul(
        for (result <- status.results) {
          li(result.toString)
        }
      )
    )
  )
}

