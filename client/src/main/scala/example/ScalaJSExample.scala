package example

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.raw.ErrorEvent
import shared.{Functions, SharedMessages, WorkItem}
import upickle.default._

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {

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