package scripts

import org.scalajs.dom.{MessageEvent, Worker}
import shared.{DoubleResult, JobID, Result}
import upickle.default._
import org.scalajs.dom

import scalatags.Text.all._
import scala.scalajs.js

object IndexPage extends js.JSApp {
  def main(): Unit = {

    println("running main")
    val worker = new Worker("/assets/clientworker-fastopt.js")

    // start off worker
    worker.postMessage(null)
    
    worker.onmessage = { e: Any =>
      val messageEvent = e.asInstanceOf[MessageEvent]
      val result = read[DoubleResult](messageEvent.data.toString)
      this.addJobIdToWorklist(result.id)
      // worker finished, start off next one
      worker.postMessage({})
    }
  }

  private def addJobIdToWorklist(id: JobID): Unit = {
    val listEntry = li(id.value)
    dom.document.getElementById("worklist").innerHTML += listEntry
  }
}