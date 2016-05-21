package hitb

import org.scalajs.dom
import shared.WorkItem
import upickle.default._

import scala.scalajs.js

object Worker extends js.JSApp {
  val self = js.Dynamic.global

  def main(): Unit = {
    println("in scalajs worker")

    val xhr = new dom.XMLHttpRequest()
    xhr.open("GET",
      "/getWorkItem"
    )
    xhr.onload = { (e: dom.Event) =>
      if (xhr.status == 200) {
          val workItem = read[WorkItem](xhr.responseText)
          js.eval(workItem.jsCode)
      } else {
        dom.document.getElementById("scalajsShoutOut").innerHTML =
          xhr.responseText
      }
    }
    xhr.send()
  }
}