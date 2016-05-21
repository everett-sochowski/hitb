package example

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.raw.ErrorEvent
import shared.{Functions, SharedMessages}

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {

    val xhr = new dom.XMLHttpRequest()
    xhr.open("GET",
      "/test"
    )
    xhr.onload = { (e: dom.Event) =>
      if (xhr.status == 200) {
        dom.document.getElementById("scalajsShoutOut").textContent =
          xhr.responseText
      } else {
        dom.document.getElementById("scalajsShoutOut").innerHTML =
          xhr.responseText
      }
    }
    xhr.send()
  }
}