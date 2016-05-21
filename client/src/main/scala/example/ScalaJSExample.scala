package example

import scala.scalajs.js
import org.scalajs.dom
import org.scalajs.dom.Worker
import org.scalajs.dom.raw.ErrorEvent
import shared.{Functions, SharedMessages, WorkItem}
import upickle.default._

object ScalaJSExample extends js.JSApp {
  def main(): Unit = {

    println("running main")
    new Worker("/assets/clientWorker-fastopt.js")
  }
}