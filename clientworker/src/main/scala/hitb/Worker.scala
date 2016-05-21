package hitb

import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import shared.{DoubleResult, Result, ReturnDoubleWorkItem, WorkItem}
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

        val computationResult = js.eval(workItem.jsCode)

        workItem match {
          case d: ReturnDoubleWorkItem => postDoubleResult(workItem, computationResult.asInstanceOf[Double])
        }
      }
  }

  private def postDoubleResult(workItem: WorkItem, computationResult: Double) = {
    val result = DoubleResult(workItem.id, computationResult)
    Ajax.post("/postResult", write(result))
  }
}