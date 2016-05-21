package scripts

import org.scalajs.dom
import shared.JobsStatus
import upickle.default._

import scala.scalajs.js.annotation.JSExport


@JSExport
object StatusPage {
  @JSExport
  def run(): Unit = {
    val xhr = new dom.XMLHttpRequest()
    xhr.open("GET", "/statusJSON")
    xhr.onload = { (e: dom.Event) =>
      if (xhr.status == 200) {
        val status = read[JobsStatus](xhr.responseText)
        dom.document.body.innerHTML = betterviews.StatusPageView.content(status).render
      }
      dom.setTimeout(() => run(), 1000)
    }
    xhr.send()
  }
}

