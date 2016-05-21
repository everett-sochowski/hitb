package betterviews

import controllers.WorkQueue

import scalatags.Text.all._


object StatusPage {
  def view(): String = {
    val status = WorkQueue.status
    Main("Status")(
      h1("Status"),
      strong("Unassigned work items: "), status.workItems, br,
      strong("Pending jobs: "), status.pendingJobs, br,
      strong("Completed jobs: "), status.results.size,
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
}

