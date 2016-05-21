package betterviews

import shared.{DoubleResult, JobsStatus, Result}

import scalatags.Text.all._


object IndexView {
  def static(): String = Main("hitb")(onload:="scripts.IndexPage().main()", content())

  def content() = div(
    h1("Welcome, voluntary worker =)"),
    h2("Your work"),
    ul(id := "worklist")
  )
}

