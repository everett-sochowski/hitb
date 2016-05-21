package hitb

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import shared.{Result, WorkItem}
import upickle.default._

import scala.concurrent._
import ExecutionContext.Implicits.global

import scala.scalajs.js

object Worker extends js.JSApp {
  val self = js.Dynamic.global

  def main(): Unit = {
    println("in scalajs worker")

    this.processNextWorkItem()
  }

  private def processNextWorkItem() = {
    Ajax
      .get("/getWorkItem")
      .onSuccess { case xhr =>
        val workItem = read[WorkItem](xhr.responseText)
        val computationResult = js.eval(workItem.jsCode).asInstanceOf[Double]
        this.postResult(workItem, computationResult)
      }
  }

  private def postResult(workItem: WorkItem, computationResult: Double) = {
    val result = Result(workItem.id, computationResult)
    Ajax.post("/postResult", write(result))

    val xhr = new dom.XMLHttpRequest()
    xhr.open("POST", "/postResult")
  }
}